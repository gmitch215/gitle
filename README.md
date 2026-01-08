# gitle

> 📥 Download Maven & Gradle Artifacts from any Git Repository

Gitle is a gradle plugin that allows you to download artifacts from any git repository. 
It is useful when you want to download artifacts from a repository that can't be accessed via Maven.

## Usage

Gitle supports all Git repositories that are accessible via HTTPS or SSH.

> [!NOTE]
> Project dependencies must be built using Maven or Gradle.
>
> If you are using gradle, make sure you set up the `maven-publish` plugin to publish the artifacts to maven local.

Simply use the `import` function to download the artifact from the repository. It will then automatically clone the
repository and add its artifacts to your maven local repository.

```kotlin
import xyz.gmitch215.gitle.import
import xyz.gmitch215.gitle.github

plugins {
    id("xyz.gmitch215.gitle") version "[VERSION]"
}

dependencies {
    import("https://github.com/example/repo.git")
    import(github("OtherUser", "OtherRepository"))
    import(gitlab("Company/PrivateRepo/Project"), token = "super-secret-token")
    
    implementation("com.example:artifact:1.0.0")
}
```

See the [wiki](https://github.com/gmitch215/gitle/wiki) for more information.

## Documentation

Dokka documentation is available [here](https://gmitch215.github.io/gitle/), or on the URL associated with this repository.

## Contributing

If you have any ideas for improvements or new features, feel free to open an issue or a pull request. All details are in the [CONTRIBUTING.md](./CONTRIBUTING.md) file.

## License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.
