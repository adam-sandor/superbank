const keycloak = new Keycloak({
    url: 'https://banking-demo.expo.styralab.com/auth',
    realm: 'banking-demo',
    clientId: 'banking-demo-portal'
});

const tableBackground = 'bg-info' // `info` is teal in bootstrap

const buttons = [ '#account-details', '#account-transactions', '#account-block']

function readyFn() {
    if (isLocalDevMode()) {
        console.log("Local development mode active")
        $("#localdevindicator").show();
        permissionsReady({
            permissions: ["account/details", "account/transactions", "account/block"],
            subject: {
                fullname: "Agent Smith",
                role_level: 3,
                geo_region: "EU"
            }
        })
    } else {
        keycloak.onAuthError = function (errorData) {
            console.log("Auth Error: " + JSON.stringify(errorData));
        };

        keycloak.init({
            enableLogging: true,
            onLoad: 'login-required'
        }).then(function (authenticated) {
            console.log(authenticated ? 'authenticated' : 'not authenticated');
            if (authenticated) {
                $.ajax({
                    type: "GET",
                    url: "/permissions",
                    headers: {"Authorization": "Bearer " + keycloak.token},
                    success: permissionsReady,
                    error: permissionsCallError
                });

                const logout = $("#logout");
                logout.show();
                logout.click(function () {
                    keycloak.logout();
                });
            }
        }).catch(function () {
            console.log('failed to initialize');
        });
    }
}

function permissionsReady(data) {
    if (data.permissions.includes('account/details')) {
        $("#account-details").show()
    }
    if (data.permissions.includes('account/transactions')) {
        $("#account-transactions").show()
    }
    if (data.permissions.includes('account/block')) {
        $("#account-block").show()
    }
    $('#user-full-name').text(data.subject.fullname);
    $('#user-role').text("Customer Support");
    $('#user-role-level').text("Level " + data.subject.role_level);
    $('#user-geo-region').text(data.subject.geo_region);
    if (data.subject.fullname === "Agent Brown") {
        $('#agent-pic').attr('src','img/agent-brown.png')
        $('#agent-pic').show()
    }
    if (data.subject.fullname === "Agent Smith") {
        $('#agent-pic').attr('src','img/agent-smith.png')
        $('#agent-pic').show()
    }
    if (data.subject.fullname === "Agent Jones") {
        $('#agent-pic').attr('src', 'img/agent-jones.png')
        $('#agent-pic').show()
    }
}

function permissionsCallError(data) {
   $("#error").show();
   $("#error .error-text").text("Failed to load permissions (" + data.statusText + " [" + data.status + "])");
}

function highlightButton(id) {
    buttons.forEach((btnId) => {
        if (btnId === id) {
            $(btnId).addClass('btn-primary')
            $(btnId).removeClass('btn-default')
        } else {
            $(btnId).addClass('btn-default')
            $(btnId).removeClass('btn-primary')
        }
    })
}

$('#account-details').click(function () {
    const ready = function (data) {
        $("#error").hide();
        highlightButton('#account-details')
        const htmlTable = `<table class="table table-bordered table-striped ${tableBackground}">
            <tbody>
            <tr>
                <td>IBAN:</td>
                <td>${data.iban}</td>
            </tr>
            <tr>
                <td>Account Holder:</td>
                <td>${data.accountHolder.name}</td>
            </tr>
            <tr>
                <td>Address: </td>
                <td>${data.accountHolder.address}</td>
            </tr>
            <tr>
                <td>Geographical Region:</td>
                <td>${data.geoRegion}</td>
            </tr>
            </tbody>
        </table>`;

        $('#data-table').html(htmlTable);
    };

    const error = function (data) {
        displayError(data, "account details")
    }
    if (isLocalDevMode()) {
        ready(mockDetails);
    } else {
        $.ajax({
            type: "GET",
            url: accountServiceUrl() + $('#account-iban-input').val() + "/details",
            headers: {"Authorization": "Bearer " + keycloak.token},
            success: ready,
            error: error
        })
    }
});

$('#account-transactions').click(function () {
    const ready = function (data) {
        $("#error").hide();
        highlightButton('#account-transactions')
        const currencyProps = {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
            style: 'currency',
            currency: 'USD'
        }

        const tr = (t) => `<tr>
            <td>${t.otherAccountIban}</td>
            <td class="text-right">${t.amount.toLocaleString(undefined, currencyProps)}</td>
            <td>${t.timeStamp}</td>
            <td>${t.type}</td>
            <td>${t.result}</td>
            <td>${t.comment ? t.comment : ""}</td>
            </tr>`
        const htmlRows = data.transactionList.map(tr).join('')

        const htmlTable = `<table class="table table-bordered table-striped ${tableBackground}">
            <thead>
                <tr class="bg-primary">
                  <th scope="col">Other Account</th>
                  <th scope="col">Amount</th>
                  <th scope="col">Time</th>
                  <th scope="col">Type</th>
                  <th scope="col">Result</th>
                  <th scope="col">Comment</th>
                </tr>
            </thead>
            <tbody>
                ${htmlRows}
            </tbody>
        </table>`;

        $('#data-table').html(htmlTable);
    };

    const error = function (data) {
        displayError(data, "account transactions");
    }

    if (isLocalDevMode()) {
        ready(mockTransactions);
    } else {
        $.ajax({
            type: "GET",
            url: accountServiceUrl() + $('#account-iban-input').val() + "/transactions",
            headers: {"Authorization": "Bearer " + keycloak.token},
            success: ready,
            error: error
        })
    }
});

$('.accountnr').click(function () {
   $('#account-iban-input').val($(this).text());
   $('#account-details').click();
});

function accountServiceUrl() {
    if (window.location.hash === '#account-controller-opa-disabled') {
        return '/account/'
    } else {
        return '/account/v2/'
    }
}

function displayError(data, operation) {
    $("#error").show();
    // Clear display from previous account, if any
    $('#data-table').html('')
    const parent = $("#error .error-text");
    parent.empty();
    if (data.status === 403) {
        parent.append(`<p>Not authorized to load ${operation} for account ${$('#account-iban-input').val()}:</p><ul>`)
        data.responseJSON.denyReasons.forEach(function(denyReason) {
            parent.append(`<li>${denyReason}</li>`)
        })
        parent.append("</ul>")
    } else {
        parent.text("Failed to load " + operation + " (" + data.statusText + " [" + data.status + "])");
    }
}

function isLocalDevMode() {
    return false;
    //return window.location.hostname === 'localhost'
}

$(readyFn)

mockDetails = {
    "iban": "EU12345435345435345",
    "accountHolder": {
        "name": "Mr. Anderson",
        "address": "Borgerstraat, Amsterdam"
    },
    "geoRegion": "EU"
};

mockTransactions = {
    "accountIban": "EU12345435345435345",
    "transactionList": [
        {
            "amount": 11135.2,
            "otherAccountIban": "SK54354656343444",
            "result": "SUCCESS",
            "timeStamp": "2021-12-05T10:15:30Z",
            "type": "OUTGOING",
            "comment": null
        },
        {
            "amount": 115.5,
            "otherAccountIban": "SK54354656343444",
            "result": "SUCCESS",
            "timeStamp": "2021-12-02T10:15:30Z",
            "type": "OUTGOING",
            "comment": null
        },
        {
            "amount": 35,
            "otherAccountIban": "SK54354656343444",
            "result": "SUCCESS",
            "timeStamp": "2021-12-01T08:15:30Z",
            "type": "OUTGOING"
        },
        {
            "amount": 135.2,
            "otherAccountIban": "SK54354656343444",
            "result": "FAILURE",
            "timeStamp": "2021-11-15T10:15:30Z",
            "type": "OUTGOING",
            "comment": "Insufficient funds"
        },
        {
            "amount": 135.2,
            "otherAccountIban": "SK54354656343444",
            "result": "FAILURE",
            "timeStamp": "2021-11-11T10:15:30Z",
            "type": "OUTGOING",
            "comment": "Insufficient funds"
        },
        {
            "amount": 122.1,
            "otherAccountIban": "SK54354656343444",
            "result": "FAILURE",
            "timeStamp": "2021-11-05T10:15:30Z",
            "type": "OUTGOING",
            "comment": "Insufficient funds"
        },
        {
            "amount": 900.5,
            "otherAccountIban": "SK54354656343444",
            "result": "SUCCESS",
            "timeStamp": "2021-11-02T10:15:30Z",
            "type": "OUTGOING",
            "comment": null
        }
    ]
};

