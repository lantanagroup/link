# LinkConfigurer

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 14.0.5.

## Development main

Run `ng serve` for a dev main. Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via a platform of your choice. To use this command, you need to first add a package that implements end-to-end testing capabilities.

## Deployment

Run `docker-build.bat` to build a Docker image and push it to Lantana's private registry.
Then delete the `configurer` pod from the `nhsnlink-aks` Kubernetes service (`default` namespace) to redeploy the container.

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.
