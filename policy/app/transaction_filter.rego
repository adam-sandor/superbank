package policy.app
import future.keywords.in

transaction_filter["result"] = "FAILURE" {
    input.subject.role_level < 3
}