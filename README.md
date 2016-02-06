# mr-coffee
Java .Jar Online package Manager

## Licence
This is a GPL v3 project, meaning if you use this code you MUST provide the full source
code to your project to everyone who can obtain a compiled copy if you use this.
As the author of this I am willing to provide a licence for closed source projects but
they will be treated on a case by case basis.

## Caveat

Due to the nature of the java classloader system one cannot reference a library class prior to
loading the .jar file which contains it. In the example below we load the imaginary library
AppToolkit.jar and then start using it.

For Example:

### Test.java
```
public class Main {
   public static void main(String args[]) {
      MrCoffee loader = new MrCoffee();
      MrCoffee.setApplicationId("P0ssess3d.net UI Toolkit");
      loader.requestJar("http://private.p0ssess3d.net/AppToolkit.jar");
      loader.requestJar("http://private.p0ssess3d.net/OtherDependancy.jar");  // Example of other dependancy
      loader.loadBasic();
      MyApplication app = new MyApplication();
      app.start();
   }
}
```

### MyApplication.java
```
import net.p0ssess3d.private.ui.toolkit.FolderSearch;

public class MyApplication {
   public MyApplication() {
   }
   public void start() {
      FolderSearch scanner = new FolderSearch("${USERHOME}");
      scanner.listFiles("*.java");
   }
}
```
So from the IDE you would need AppToolkit.jar to be in your classpath, but when your application
is distributed you would only need to upload your main .jar and have users of your application simply download
a single file and double click on it.


## Usage
To use this in your project you simply include MrCoffee.java in your source tree and include it.

Once included follow the examples provided in MrCoffeeExamples.java. There is javadoc provided
for all public methods and classes to assist.

Once used properly this can allow your project to be distributed as a standalone .jar file
with all dependant .jar files being downloaded and stored as needed.

There is a advanced example which shows how Mr. Coffee can be used to provide user feedback.

