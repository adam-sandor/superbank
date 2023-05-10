package permissions

ui_permissions["account/details"] {
    jwt.role_level >= 1
}
ui_permissions["account/transactions"] {
    jwt.role_level >= 2
}
ui_permissions["account/block"] {
    jwt.role_level >= 5
}

default allowed = false
allowed {
  input.action
  ui_permissions[input.action]
}

allowed {
  not input.action
  count(ui_permissions) > 0
}

jwt = {"username": username, "role_level": role_level} {
  [_, payload, _] := io.jwt.decode(input.jwt)
  username := payload.preferred_username
  role_level := payload.role_level
}