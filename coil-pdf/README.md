# PDFs

To add PDF page support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-pdf:1.1.0")
```

And add the two fetchers to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .componentRegistry {
         add(PdfPageFileFetcher())
         add(PdfPageUriFetcher())
    }
    .build()
```

!!! Note
    PDF page rendering is only supported for `File`s and `Uri`s (`content` and `file` schemes only) on Android 5.0+.

To specify the background color to render a PDF on, use `pdfBackgroundColor`:

```kotlin
imageView.load(File("/path/to/document.pdf")) {
    pdfBackgroundColor(Color.TRANSPARENT)
}
```

If a background color isn't specified, the PDF is rendered on a white background.

To specify the page index to render from a PDF, use `pdfPageIndex`:

```kotlin
imageView.load(File("/path/to/document.pdf")) {
    pdfPageIndex(1)
}
```

If a page index isn't specified, the first page (index 0) of the PDF is rendered.

The `ImageLoader` will automatically detect any PDFs and extract their pages if the request's filename/URI ends with the `pdf` extension. If it does not, you can [set the `Fetcher` explicitly](../api/coil-base/coil.request/-image-request/-builder/fetcher/) for the request.
