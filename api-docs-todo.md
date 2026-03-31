# API Documentation for ToDo Application

## Endpoints

All endpoints are prefixed with `/todo/api/1`. Most of these endpoints require authentication, usually provided via headers or an authentication token. 

The `id` parameter used in path URLs should correspond to a valid Task ID.

### 1. Get Tasks
- **URL**: `/todo/api/1/tasks`
- **Method**: `GET`
- **Description**: Returns a list of tasks for the authenticated user.
- **Request Body**: None.
- **Response**: `List<TaskDto>`

### 2. Copy Task
- **URL**: `/todo/api/1/tasks/{id}/copy`
- **Method**: `GET`
- **Description**: Creates a copy of the task identified by `{id}` for the authenticated user.
- **Request Body**: None.
- **Response**: `TaskDto`

### 3. Add Task
- **URL**: `/todo/api/1/tasks`
- **Method**: `POST`
- **Description**: Creates a new task.
- **Request Body**: `CreateTaskDto`
- **Response**: `TaskDto`

### 4. Update Task
- **URL**: `/todo/api/1/tasks/{id}`
- **Method**: `PATCH`
- **Description**: Updates an existing task identified by `{id}`. Can update fields like description, parent task, priority, etc. 
- **Request Body**: `UpdateTaskDto`
- **Response**: `TaskDto`

### 5. Remove Task
- **URL**: `/todo/api/1/tasks/{id}`
- **Method**: `DELETE`
- **Description**: Removes the task identified by `{id}`. Use query parameters to control removal with subtasks if needed.
- **Request Body**: None.
- **Response**: `TaskDto` (Returns the deleted task)

### 6. Mark Task as To Do / Done
- **URL**: `/todo/api/1/tasks/mark-as-to-do`
- **Method**: `POST`
- **Description**: Marks a list of tasks as either "to-do" or "done".
- **Request Body**: `MarkAsToDoDto`
- **Response**: `List<TaskDto>`

### 7. Leave Share
- **URL**: `/todo/api/1/tasks/leave-share`
- **Method**: `POST`
- **Description**: Removes the authenticated user from a shared task.
- **Request Body**: `LeaveShareDto`
- **Response**: `TaskDto`

---

## User Endpoints

These endpoints are used to get user data which is relevant when sharing tasks with other users.

### 1. Get Users
- **URL**: `/fiteo/api/1/users`
- **Method**: `GET`
- **Description**: Returns a list of all users in the system.
- **Request Body**: None.
- **Response**: `List<UserDto>`

---

## Data Models (DTOs)

### `TaskDto`
Represents a fully populated Task object.
```json
{
    "id": 1,
    "ownerId": 123,
    "description": "Task Description",
    "addedTime": "2023-01-01T12:00:00Z",
    "modifiedTime": "2023-01-02T12:00:00Z",
    "parentTaskId": null,
    "subTasks": [],
    "isToDo": true,
    "priority": 1,
    "scheduler": { /* SchedulerDto */ },
    "expirationDate": "2023-12-31T23:59:59Z",
    "shares": [
        {
            "userId": "456",
            "permission": "READ"
        }
    ]
}
```

### `CreateTaskDto`
Used when creating a new task.
```json
{
    "description": "New Task Description",
    "parentTaskId": null, // Optional ID of the parent task
    "priority": 1, // Optional priority
    "highestPriorityAsDefault": true, // Optional flag
    "scheduler": null, // Optional SchedulerDto
    "isToDo": true, // Optional flag indicating if the task is to-do or done
    "expirationDate": "2023-12-31T23:59:59Z", // Optional expiration date for the task
    "shares": [ // Optional sharing configuration
        {
            "userId": "456",
            "permission": "EDITOR_AND_VIEWER"
        }
    ]
}
```

### `UpdateTaskDto`
Used to update an existing task. Most fields are optional and are only updated if provided.
```json
{
    "description": "Updated Description", // Optional
    "parentTaskId": { "value": null }, // Optional, wrapped to allow explicitly setting to null
    "priority": 2, // Optional
    "isToDo": false, // Optional
    "scheduler": { "value": null }, // Optional, wrapped to allow explicitly setting to null
    "expirationDate": { "value": "2023-12-31T23:59:59Z" }, // Optional, explicitly set expiration date
    "shares": [ // Optional
        {
            "userId": "456",
            "permission": "EDITOR_AND_VIEWER"
        }
    ]
}
```

### `TaskShareDto`
Describes sharing permission for a specific user on a task.
```json
{
    "userId": "456", // ID of the user the task is shared with
    "permission": "EDITOR_AND_VIEWER" // The level of permission (e.g., EDITOR_AND_VIEWER)
}
```

### `UpdateTaskShareDto`
Describes sharing configuration when modifying shares.
```json
{
    "userId": "456",
    "permission": "EDITOR_AND_VIEWER"
}
```

### `LeaveShareDto`
Used to leave a shared task.
```json
{
    "id": 1 // The ID of the task to leave
}
```

### `MarkAsToDoDto`
Used to batch-update the "to-do" status of multiple tasks.
```json
{
    "isToDo": true, // The new status for the given tasks
    "taskIds": [1, 2, 3] // The IDs of the tasks to update
}
```

### `SchedulerDto`
Defines scheduling rules for tasks.
```json
{
    "hour": 9,
    "minute": 30,
    "creationDate": "2023-01-01T12:00:00Z", // Optional
    "startDate": "2023-01-01T12:00:00Z",
    "lastDate": "2024-01-01T12:00:00Z", // Optional
    "daysOfWeek": [1, 3, 5],
    "dayOfMonth": 15,
    "repeatCount": 10,
    "repeatInEveryPeriod": 1,
    "type": "WEEKLY", // Defines the recurrence pattern type
    "options": { // Optional additional options for scheduling
        "key": "value"
    }
}
```

### `UserDto`
Represents a user.
```json
{
    "id": 1,
    "email": "user@example.com"
}
```

## Important Features

### Expiration Date
Tasks have an optional `expirationDate` property (String representation of a date, typically ISO 8601).
- Accessible when reading tasks via `TaskDto`.
- Can be set upon task creation via `CreateTaskDto`.
- Can be updated via `UpdateTaskDto`. Note that in `UpdateTaskDto`, it uses a wrapped `NullableFieldDto<String>` to allow explicitly clearing the expiration date by passing `{"value": null}`.

### Task Sharing
Tasks can be shared with other users, allowing collaborative tracking.
- The `shares` field exposes a list of `TaskShareDto` entries for the task.
- `TaskShareDto` and `UpdateTaskShareDto` pair a `userId` with a specific `permission` level. You can retrieve other users by calling `/fiteo/api/1/users`.
- Users can update shares dynamically via `UpdateTaskDto` using `UpdateTaskShareDto`.
- Shared tasks can be abandoned by a participant via the `LeaveShare` endpoint, supplying the task `id` in the `LeaveShareDto`.