# Releasing

1. Update the `POM_VERSION` in `gradle.properties` to the release version.

2. Add release notes for the new version to `CHANGELOG.md`.

3. Run `./assemble_baseline_profile.sh` to ensure the baseline profiles are up to date.

4. Find and replace the previous version in the docs with the new version.

5. Commit

   ```
   $ git commit -m "Prepare version X.Y.Z."
   ```

6. Publish

   ```
   $ ./publish_remote.sh
   ```

7. Tag

   ```
   $ git tag X.Y.Z
   ```

8. Update the `POM_VERSION` in `gradle.properties` to the next "SNAPSHOT" version.

9. Commit

   ```
   $ git commit -m "Prepare next development version."
   ```

10. Push!

    ```
    $ git push && git push --tags
    ```

11. Visit [Sonatype Nexus](https://oss.sonatype.org/) and promote the artifact.

12. Create a [new Github release](https://github.com/coil-kt/coil/releases/new) that points to the new version's tag and entry in the `CHANGELOG.md`.
