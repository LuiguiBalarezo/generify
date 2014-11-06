Generify
========

Gradle plugin for Android which lets you use Java generics in your XML layouts.

Usage
-----

Apply the plugin in your `build.gradle` *after* the regular 'android' plugin:

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  
  dependencies {
    classpath 'com.android.tools.build:gradle:0.14.+'
    classpath 'com.tomreznik:generify:0.1.+'
  }
}

apply plugin: 'com.android.application'
apply plugin: 'generify'
```

Consider the following custom view class:

```java
public class AwesomeView<T> extends TextView {
  // ...
}
```

Using Generify, all you have to do is include your app's namespace and provide a `generic_type` expression, for example:

```xml
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

  <com.package.name.AwesomeView
      app:generic_type="String"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"/>

</FrameLayout>
```

At compile time, this plugin will generate a subclass of `AwesomeView` while hard-coding the generic expression:

```java
public class AwesomeViewString extends AwesomeView<String> {
  // ...
}
```

In addition, your XML references to these views will automatically be replaced with the generated ones.

Multiple types:

```xml
<com.package.name.SomeView
    app:generic_type="String,Integer"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"/>
```

Nested types:

```xml
<com.package.name.SomeView
    app:generic_type="java.util.List(String)"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"/>
```

License
-------

    Copyright 2014 Tom Reznik

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
