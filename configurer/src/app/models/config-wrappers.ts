import {
  ApiConfig,
  BundlerConfig,
  ConceptMapConfigWrapper,
  ConsumerConfig,
  FHIRSenderConfigWrapper,
  QueryConfig,
  SwaggerConfig,
  USCoreConfig
} from "./config";

export class ApiConfigWrapper  {
  swagger = new SwaggerConfig();
  api = new ApiConfig();
  applyConceptMaps = new ConceptMapConfigWrapper();
  query = new QueryConfig();
  uscore?: USCoreConfig;
  sender?: FHIRSenderConfigWrapper;
  bundler = new BundlerConfig();
}

export class ConsumerConfigWrapper  {
  consumer = new ConsumerConfig();
}
