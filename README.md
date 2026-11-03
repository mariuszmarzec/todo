# Todo application (in progress)

Created with Jetpack/JetBrains Compose playground.

Backend for project: https://github.com/mariuszmarzec/fiteo/tree/master/src/commonMain/kotlin/com/marzec/todo

# Configuration

Create `local.properties` with the app configuration:

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
./gradlew desktop:run
```

# Local MCP server (Desktop only)

The desktop app can expose its Todo tools through the Model Context Protocol using Streamable HTTP. The server binds to loopback only and is not started by Android.

## Defaults and lifecycle

The server starts only when both conditions hold: `todo.localMcpServer` is enabled and the app's current-user stream contains a user. It stops after logout, when the flag is disabled, and when the desktop application closes. In mock mode, the existing mock user is treated as signed in.

| Build environment | Default `todo.localMcpServer` value |
| --- | --- |
| `m` (mock/local) | `true` |
| `t` (test) | `false` |
| `p` (production) | `false` |

Defaults are in `multiplatformUtils/src/commonMain/composeResources/files/<environment>/feature_toggle_conf.json`. The existing remote feature-toggle mechanism can override the default in environments using the backend. The mock mode uses an empty mock feature-toggle data source, so to disable the server for a mock build, set `todo.localMcpServer` to `false` in the `m` resource and rebuild.

The current endpoint is fixed at `http://127.0.0.1:8765/mcp`. The server binds to loopback only and accepts plain, unauthenticated connections — no `Authorization` header is required. Existing tools work without any header.

## Configure OpenCode

OpenCode supports remote Streamable HTTP MCP servers. Add this entry to the project's `opencode.json` or the user-level OpenCode configuration file:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "todo-local": {
      "type": "remote",
      "url": "http://127.0.0.1:8765/mcp",
      "enabled": true,
      "oauth": false
    }
  }
}
```

Use `opencode mcp list` to verify the server connection. The official [OpenCode MCP server documentation](https://opencode.ai/docs/en/mcp-servers/) describes the remote transport and header settings.

If the server is not reachable, confirm Todo Desktop is running and the feature flag and user-session conditions are satisfied.

## Available tools and limitations

The server exposes `list_tasks`, `list_scheduled_tasks`, `get_task`, `create_task`, `create_tasks`, `create_task_tree`, `update_task`, `mark_tasks_done`, `mark_tasks_to_do`, `reorder_tasks`, `schedule_tasks`, `copy_task`, `remove_task`, `leave_shared_task`, and `list_shareable_users`, along with the `ping` connectivity check.

The `create_task` response includes the exact ID returned by the data source:
`{"success":true,"taskId":123}`. When `parentTaskId` is supplied, that ID
identifies the newly created child task.

In mock/local mode, task-sharing operations are unsupported. In particular, `leave_shared_task`, `list_shareable_users`, and create/update requests that explicitly include `shares` return a tool error rather than silently ignoring the request. The local MCP server is independent of the app's backend session — it never reads backend credentials.

## Verification

Run the Android and Desktop validation used by CI:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :multiplatformUtils:desktopTest :common:desktopTest :desktop:jvmTest
```

CI also checks that the Android runtime dependency graph does not contain the Desktop-only MCP server SDK or Ktor server artifacts. The Desktop integration tests initialize an MCP client over HTTP without authorization, discover tools, and perform a read and a write.

## Migrations

The `quickmvi-navigation` sample was migrated from this repository into the [QuickMVI](https://github.com/mariuszmarzec/QuickMVI) repository (issue #39, QuickMVI PR #28). The navigation utilities remain available as the `io.github.mariuszmarzec:quickmvi-navigation:1.3.0` artifact and are still used by this project via `multiplatformUtils`.

## Shared multiplatformUtils

The shared Kotlin Multiplatform module is checked out as a Git submodule.

For a fresh checkout, initialize it with:

```bash
git submodule sync --recursive
git submodule update --init --recursive
```

Or clone the repository with submodules enabled:

```bash
git clone --recurse-submodules https://github.com/mariuszmarzec/todo.git
```
