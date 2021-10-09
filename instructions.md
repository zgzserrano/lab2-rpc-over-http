# Solution

1. Complete the endpoint in the server to return the traslation

2. Build and run the server

    ```sh
    $ ./gradlew :server:build
    $ ./gradlew :server:bootRun
    ```

3. Build and run the client

    ```sh
    $ ./gradlew :client:build
    $ ./gradlew :client:bootRun
    ```

**NOTE**: Server must be executed before build the client

4. Configure GitHub actions to build work branch adding the following to the `ci.yml`

    ```yml
    on:
    push:
        branches: [ main, work ]
    pull_request:
        branches: [ main, work ]
    ```

