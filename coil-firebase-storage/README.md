# Firebase Storage

To add FirebaseStorage support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-firebase-storage:0.8.0")
```

And add the fetcher to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader(context) {
    componentRegistry {
        add(FirebaseImageFetcher())
    }
}
```

And that's it! The `ImageLoader` will automatically detect any Firebase Storage images using their [StorageReference](https://firebase.google.com/docs/reference/android/com/google/firebase/storage/StorageReference) and fetch them correctly.
