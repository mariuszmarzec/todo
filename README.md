# Todo application (in progress)
Created with Jetpack/Jetbrains Compose playground

Backend for project: https://github.com/mariuszmarzec/fiteo/tree/master/src/commonMain/kotlin/com/marzec/todo

# Configuration

local.properties:
```properties
storePassword=<STORE_PASSWORD>
keyAlias=<ALIAS>
keyPassword=<KEY_PASSWORD>
storeFile=<STORE FILE PATH>

# p - prod, t - test, m - mock
environment=m
prod.apiUrl=http://localhost/
prod.authHeader=Authorization
test.apiUrl=http://localhost/test
test.authHeader=Authorization-Test
```

# Run Desktop

```bash
gradlew desktop:run
```

## Migrations

The `quickmvi-navigation` sample was migrated from this repository into the [QuickMVI](https://github.com/mariuszmarzec/QuickMVI) repository (issue #39, QuickMVI PR #28). The navigation utilities remain available as the `io.github.mariuszmarzec:quickmvi-navigation:1.3.0` artifact and are still used by this project via `multiplatformUtils`.
