import {
  ApiConfig,
  BundlerConfig,
  ConceptMapConfigWrapper,
  ConsumerConfig,
  FHIRSenderConfigWrapper,
  QueryConfig,
  USCoreConfig
} from "./config";

export class ApiConfigWrapper  {
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
