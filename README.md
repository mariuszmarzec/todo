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

The current endpoint is fixed at `http://127.0.0.1:8765/mcp`. The desktop server uses the local MCP token, not the backend's login credentials. Requests without a valid `Authorization: Bearer <token>` header are rejected with HTTP 401. The server reads the current token for each request, so saving or regenerating the token immediately invalidates the previously stored value.

## Retrieve or rotate the token

1. Start Todo Desktop.
2. On the login screen, select the lock icon labelled **Local MCP token**.
3. Select **Copy token** to copy the current token. Use **Regenerate** or save a new token to invalidate the old one.
4. Store the token in a local environment variable for the MCP client. Do not commit the token to this repository or paste it into logs or issue comments.

The token is persisted in the current user's Java Preferences store. It is local to the desktop profile and is not a shared/default secret.

## Configure OpenCode

OpenCode supports remote Streamable HTTP MCP servers with custom headers. Add this entry to the project's `opencode.json` or the user-level OpenCode configuration file. Keep the token in the `TODO_LOCAL_MCP_TOKEN` environment variable rather than in the JSON file:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "todo-local": {
      "type": "remote",
      "url": "http://127.0.0.1:8765/mcp",
      "enabled": true,
      "oauth": false,
      "headers": {
        "Authorization": "Bearer {env:TODO_LOCAL_MCP_TOKEN}"
      }
    }
  }
}
```

On Windows PowerShell, enter the token without echoing it and then start OpenCode from the same terminal:

```powershell
$secureToken = Read-Host "Local Todo MCP token" -AsSecureString
$env:TODO_LOCAL_MCP_TOKEN = [System.Net.NetworkCredential]::new("", $secureToken).Password
opencode
Remove-Item Env:TODO_LOCAL_MCP_TOKEN
```

In Bash (for example, in WSL), enter it without echoing it and start OpenCode from that shell:

```bash
read -rsp "Local Todo MCP token: " TODO_LOCAL_MCP_TOKEN
echo
export TODO_LOCAL_MCP_TOKEN
opencode
unset TODO_LOCAL_MCP_TOKEN
```

Use `opencode mcp list` to verify the server connection. The official [OpenCode MCP server documentation](https://opencode.ai/docs/en/mcp-servers/) describes the remote transport and header settings.

If the token is regenerated, update the environment variable before reconnecting the client. A 401 response means the token is missing, invalid, or out of date. Confirm Todo Desktop is running and the feature flag and user-session conditions are satisfied.

## Available tools and limitations

The server exposes `list_tasks`, `list_scheduled_tasks`, `get_task`, `create_task`, `create_tasks`, `create_task_tree`, `update_task`, `mark_tasks_done`, `mark_tasks_to_do`, `reorder_tasks`, `schedule_tasks`, `copy_task`, `remove_task`, `leave_shared_task`, and `list_shareable_users`, along with the `ping` connectivity check.

The `create_task` response includes the exact ID returned by the data source:
`{"success":true,"taskId":123}`. When `parentTaskId` is supplied, that ID
identifies the newly created child task.

In mock/local mode, task-sharing operations are unsupported. In particular, `leave_shared_task`, `list_shareable_users`, and create/update requests that explicitly include `shares` return a tool error rather than silently ignoring the request. The local MCP token is separate from the app's backend session, and its possession grants access to the MCP tools available in that running desktop session.

## Verification

Run the Android and Desktop validation used by CI:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :multiplatformUtils:desktopTest :common:desktopTest :desktop:jvmTest
```

CI also checks that the Android runtime dependency graph does not contain the Desktop-only MCP server SDK or Ktor server artifacts. The authenticated Desktop integration tests initialize an MCP client over HTTP, discover tools, perform a read and a write, and verify missing, invalid, and rotated tokens are rejected.

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
