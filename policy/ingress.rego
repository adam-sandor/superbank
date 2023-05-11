package policy.ingress

import future.keywords.in
import input.attributes.destination.principal as destination
import input.attributes.source.principal as source

default allow = false

allow {
  destination == spiffe("portal")
}

allow {
  destination == spiffe("status")
}

allow {
  destination == spiffe("permissions")
  jwt
}

allow {
  destination == spiffe("account")
  input.parsed_path = ["account", "v2", account, "details"]
  "customer_support" in jwt.realm_access.roles
  jwt.role_level >= 2
}

allow {
  destination == spiffe("account")
  input.parsed_path = ["account", "v2", account, "transactions"]
  "customer_support" in jwt.realm_access.roles
  jwt.role_level >= 3
}

allow {
  #account service is allowed to call accountholder service
  source == spiffe("account")
  destination == spiffe("accountholder")
}

allow {
  #status service is allowed to access /status endpoint on any service
  source == spiffe("status")
  input.parsed_path[0] == "status"
}

spiffe(service) = id {
    id := concat("", ["spiffe://cluster.local/ns/banking-demo/sa/", service, "-sa"])
}

jwt := payload {
  [_, payload, _] := io.jwt.decode(bearer_token)
}

bearer_token := t {
  v := input.attributes.request.http.headers.authorization
  startswith(v, "Bearer ")
  t := substring(v, count("Bearer "), -1)
}