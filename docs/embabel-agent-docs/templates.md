Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.11. Templates
Embabel supports Jinja templates for generating prompts.
You do this via the `PromptRunner.rendering(String)` method.This method takes a Spring resource path to a Jinja template.
The default location is under `classpath:/prompts/` and the `.jinja` extension is added automatically.You can also specify a full resource path with Spring resource conventions.Once you have specified the template, you can create objects using a model map.An example:JavaKotlin
#### 4.11.1. Custom Template Renderer
By default, `rendering()` uses the platform’s `TemplateRenderer` (a Jinja-based renderer that loads templates from the classpath).
You can override this on a per-rendering basis using `withTemplateRenderer()`, which lets you supply a custom `TemplateRenderer` implementation.This is useful when you need to load templates from a different source—for example, pulling templates from a user-specific directory on the file system, or from a database, enabling per-tenant or per-user prompt customization.JavaKotlin
| | Don’t rush to externalize prompts.
In modern languages with multi-line strings, it’s often easier to keep prompts in the codebase.
Externalizing them can sacrifice type safety and lead to complexity and maintenance challenges. |