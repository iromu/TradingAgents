Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

#### 4.5.1. Enabling Embabel
Annotate your Spring Boot application class to get agentic behavior.Example:JavaKotlinThis is a normal Spring Boot application class.
You can add other Spring Boot annotations as needed.You also need to add the dependency and configuration for your LLM provider(s) of choice.


#### 4.5.2. Configuration Properties
The following table lists all available configuration properties in Embabel Agent Platform.
Properties are organized by their configuration prefix and include default values where applicable.
They can be set via `application.properties`, `application.yml`, profile-specific configuration files or environment variables, as per standard Spring configuration practices.
##### Setting default LLM and roles
From `ConfigurableModelProviderProperties` - configuration for default LLMs and role-based model selection.
| Property | Type | Default | Description || `embabel.models.default-llm` | String | `gpt-4.1-mini` | Default LLM name. It’s good practice to override this in configuration. || `embabel.models.default-embedding-model` | String | `null` | Default embedding model name. Need not be set, in which case it defaults to null. || `embabel.models.llms` | Map<String, String> | `{}` | Map of role to LLM name. Each entry will require an LLM to be registered with the same name. May not include the default LLM. || `embabel.models.embedding-services` | Map<String, String> | `{}` | Map of role to embedding service name. Does not need to include the default embedding service. You can create as many roles as you wish. |
Role-based model selection allows you to assign specific LLMs or embedding services to different roles within your application.
For example:It’s good practice to decouple your code from specific models in this way.
