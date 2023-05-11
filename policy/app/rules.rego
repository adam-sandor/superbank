package policy.app
import future.keywords.in

default allowed = false
allowed = true {
    count(deny) == 0
}

deny[message] {
    not "customer_support" in input.subject.roles
    message := "missing customer_support role"
}

deny[message] {
    input.subject.role_level < 2
    message := sprintf("role level too low %v", [input.subject.role_level])
}

deny[message] {
    input.subject.geo_region != input.account.geo_region
    message := sprintf("Geo region of customer support employee (%v) doesn't match account's (%v)", [input.subject.geo_region, input.account.geo_region])
}
