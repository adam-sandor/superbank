package superbank.permissions;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.bisnode.opa.client.OpaClient;
import com.bisnode.opa.client.query.QueryForDocumentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import superbank.permissions.entities.*;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
class AccountControllerWithOpa {

	private final OpaClient opaClient;

	private final AccountRepository accountRepository;

	private final TransactionRepository transactionRepository;

	@Value("${ACCOUNT_HOLDER_URL:http://accountholder/accountholder}")
	private String accountHolderUrl = "";

	private final HttpClient httpClient = HttpClient.newBuilder().build();

	private static final Logger log = LoggerFactory.getLogger(AccountControllerWithOpa.class);

	public AccountControllerWithOpa(@Autowired OpaClient opaClient,
                                    @Autowired AccountRepository accountRepository,
                                    @Autowired TransactionRepository transactionRepository) {
		this.opaClient = opaClient;
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
	}

	@GetMapping("/account/v2/{location}/{accountIban}/details")
	ResponseEntity<?> accountDetails(@PathVariable(name = "location") String location,
									 @PathVariable(name = "accountIban") String accountIban,
									 @RequestHeader(name = "Authorization") String authHeader)
			throws Exception {
		DecodedJWT jwt = JWT.decode(AuthHeader.getBearerToken(authHeader));
		Optional<Account> account = accountRepository.findAccountByIban(accountIban);
		log.info("Retrieving account details for account {} by subject {}", accountIban, jwt.getClaim("sub").asString());
		if (account.isPresent()) {
			log.info("Requesting account holder for account {}", accountIban);
			URI uri = new URI(accountHolderUrl + "/" + account.get().getAccountHolderId());
			HttpRequest request = HttpRequest.newBuilder()
					.uri(uri)
					.GET()
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() >= 300) {
				log.error("Error getting account holder for account {}: code={} resp-body={}", uri, response.statusCode(), response.body());
				return new ResponseEntity<>(
						new AccountWithHolder(account.get(), new AccountHolder("Unauthorized", "Unauthorized")),
						HttpStatus.OK);
			}
			List<String> denyReasons = authorizeAccountWithOpa(jwt, account.get());
			if (denyReasons.size() == 0) {
				return new ResponseEntity<>(
						new AccountWithHolder(account.get(), new AccountHolder(response.body())),
						HttpStatus.OK);
			} else {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new DenyResponse(denyReasons));
			}
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@GetMapping("/account/v2/{location}/{accountIban}/transactions")
	ResponseEntity<?> accountTransactions(
			@PathVariable(name = "location") String location,
			@PathVariable(name = "accountIban") String accountIban,
			@RequestHeader(name = "Authorization") String authHeader) {
		DecodedJWT jwt = JWT.decode(AuthHeader.getBearerToken(authHeader));
		Optional<Account> account = accountRepository.findAccountByIban(accountIban);

		if (!account.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorNode("Account " + accountIban + " not found"));
		}

		ObjectNode opaInput = createOpaInputWithAccountAndSubject(jwt, account.get());
		QueryForDocumentRequest opaQuery = new QueryForDocumentRequest(opaInput, "policy/app");
		ObjectNode opaResult = opaClient.queryForDocument(opaQuery, ObjectNode.class);
		log.info("Received response from OPA: {}", opaResult);
		List<String> denyReasons = new ArrayList<>();
		opaResult.get("deny").elements().forEachRemaining((denyReason) -> denyReasons.add(denyReason.asText()));
		if (denyReasons.size() > 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new DenyResponse(denyReasons));
		} else {
			JsonNode transactionFilters = opaResult.get("transaction_filter");

			TransactionType typeFilter = transactionFilters.get("type") != null ? TransactionType.valueOf(transactionFilters.get("type").asText()) : null;
			TransactionResult resultFilter = transactionFilters.get("result") != null ? TransactionResult.valueOf(transactionFilters.get("result").asText()) : null;
			List<Transaction> dbTransactions = transactionRepository.findTransactions(account.get(), resultFilter, typeFilter);

			ObjectMapper mapper = new ObjectMapper();
			ObjectNode response = mapper.createObjectNode();
			response.put("accountIban", accountIban);
			var transactionsNode = mapper.createArrayNode();
			dbTransactions.forEach(t -> {
				var transaction = new ObjectMapper().createObjectNode();
				transaction.put("otherAccountIban", t.getOtherAccountIban());
				transaction.put("amount", t.getAmount());
				transaction.put("timeStamp", t.getTimeStamp().toString());
				transaction.put("type", t.getType().name());
				transaction.put("result", t.getResult().name());
				transaction.put("comment", t.getComment());
				transactionsNode.add(transaction);
			});
			response.set("transactionList", transactionsNode);
			return ResponseEntity.ok(response);
		}
	}

	@GetMapping("/account/v2/{location}/{accountIban}/block")
	ResponseEntity<?> accountBlock(
			@PathVariable(name = "location") String location,
			@PathVariable(name = "accountIban") String accountIban,
			@RequestHeader(name = "Authorization") String authHeader) {
		return ResponseEntity.ok().build();
	}


	private ObjectNode createOpaInputWithAccountAndSubject(DecodedJWT jwt, Account account) {
		ObjectNode input = new ObjectMapper().createObjectNode();
		ObjectNode subject = new ObjectMapper().createObjectNode();
		subject.put("sub", jwt.getClaim("sub").asString());
		subject.put("user_id", jwt.getClaim("preferred_username").asString());
		subject.put("role_level", jwt.getClaim("role_level").asInt());
		subject.put("geo_region", jwt.getClaim("geo_region").asString());
		input.set("subject", subject);

		ObjectNode accountNode = new ObjectMapper().createObjectNode();
		accountNode.put("iban", account.getIban());
		accountNode.put("geo_region", account.getGeoRegion());
		// accountNode.putPOJO("account_holder", account.getAccountHolder());
		input.set("account", accountNode);

		List<String> roles = (List<String>) jwt.getClaim("realm_access").asMap().get("roles");
		ArrayNode rolesNode = new ObjectMapper().createArrayNode();
		roles.forEach(role -> rolesNode.add(role));
		subject.set("roles", rolesNode);
		return input;
	}

	private List<String> authorizeAccountWithOpa(DecodedJWT jwt, Account account) {
		ObjectNode input = createOpaInputWithAccountAndSubject(jwt, account);
		QueryForDocumentRequest opaQuery = new QueryForDocumentRequest(input, "policy/app");
		ObjectNode opaResult = opaClient.queryForDocument(opaQuery, ObjectNode.class);

		JsonNode deny = opaResult.get("deny");
		List<String> denyReasons = new ArrayList<>();
		deny.elements().forEachRemaining((denyElement) -> {
			denyReasons.add(denyElement.asText());
		});
		if (deny.size() > 0) {
			for (int i = 0; i < deny.size(); i++) {
				log.info("OPA authz deny: {}", deny.get(i).asText());
			}
		}
		return denyReasons;
	}

	private static ObjectNode errorNode(String error) {
		ObjectNode node = new ObjectMapper().createObjectNode();
		node.put("message", error);
		return node;
	}
}
