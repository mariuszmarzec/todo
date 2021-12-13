# Todo application (in progress)
Created with Jetpack/Jetbrains Compose playground

Backend for project: https://github.com/mariuszmarzec/fiteo/tree/master/common/src/commonMain/kotlin/com/marzec/todo

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