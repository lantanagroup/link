# Link
[![Build status](https://dev.azure.com/lantanagroup/nhsnlink/_apis/build/status/CI%20Build)](https://dev.azure.com/lantanagroup/nhsnlink/_build/latest?definitionId=46)
## Usage & Deployment

See [Deployment](https://github.com/lantanagroup/link/wiki/Deployment) in the WIKI.

### Docker Development Testing

To run the Link application in a Docker container, use the following command:

```bash
docker run -d --name link-api --mount type=bind,source="c:\...\application.yml",target=/application.yml,readonly -e SPRING_CONFIG_LOCATION=file:///application.yml link-api
```

## Configuration

See [Configuration](https://github.com/lantanagroup/link/wiki/Configuration) in the WIKI.

## Development
See [Development](https://github.com/lantanagroup/link/wiki/Development) in the WIKI.
