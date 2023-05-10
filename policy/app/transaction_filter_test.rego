package policy.app

test_transaction_filter_for_level_2 {
    filters := transaction_filter with input as {
        "subject": {
            "role_level": 2
        }
    }

    print(filters)
    filters == {
       "result": "FAILURE"
    }
}

test_transaction_filter_for_level_3 {
    filters := transaction_filter with input as {
        "subject": {
            "role_level": 3
        }
    }

    print(filters)
    filters == {
    }
}