![Coil](logo.svg)

یک کتابخانه‌ی بارگیری تصاویر برای [Android](https://www.android.com/) و [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/). کویل هست:

- **سریع**: کویل، تعدادی بهینه‌سازی از جمله ذخیره‌سازی (کش‌کردن) حافظه، نمونه‌برداری [کاهشی] از تصویر، مکث و یا لغو خودکار درخواست‌ها و غیره انجام می‌هد.
- **سبک**: کویل فقط به کاتلین، Coroutineها و Okio وابسته است و به‌طور یکپارچه با R8 code shrinker از گوگل کار می‌کند.
- **آسان برای استفاده**: واسط برنامه‌نویسی (API) کویل، از ویژگی‌های زبان Kotlin برای سادگی و جلوگیری از کدهای تکراری بهره می‌برد.
- **نوین**: کویل مبتنی بر Kotlin است و با کتابخانه‌های جدید از جمله Compose، کوروتین‌ها، اوکیو، OkHttp و Ktor کار می‌کند.

کویل (Coil) مخفف عبارت ِ **Co**routine **I**mage **L**oader است.

## شروع فوری

کتابخانه‌ی Compose و یک [networking library](https://coil-kt.github.io/coil/network/) را وارد (Import) کنید:

```kotlin
implementation("io.coil-kt.coil3:coil-compose:3.3.0")
implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
```

برای بارگیری یک تصویر، `AsyncImage` composable را استفاده کنید:
```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```

مستندات کامل Coil را [از اینجا](https://coil-kt.github.io/coil/getting_started/) بخوانید.

## پروانه

    Copyright 2025 Coil Contributors

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
