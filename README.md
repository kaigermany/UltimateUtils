# UltimateUtils
This is a wide collection of code that you might need from time to time.
It is based on my old library, but here is a fundamental redesign of it.

Note that the license is not yet set.

# How to use
I don't explain every detail here, but a few example cases.
## ClassExtensions
You can now include ClassExtensions in your class, for example if you run smaller projects, tests or such.
```
class MyNewClass implements ClassExtensions{
  public MyNewClass(File file) throws IOException {
    byte[] bytes = this.loadBytes(file);
    if(bytes.length >= 4){
      String header = new String(bytes, 0, 4);
      if(header.conatins("png")) {
        System.out.println("i found a PNG header!");
        return;
      } else if(header.conatins("pdf")) {
        System.out.println("i found a PNG header!");
        return;
      }
    }
    System.out.println("i don't know that header!");
    this.binaryDump(bytes);
  }
}
```
This example takes a file and read it. Next, it checks the causual Strings "png" and "pdf".
If none of them triggers, the fallback will print a well known formated hex dump of the loaded bytes into std-out.

Furter, you can reduce the includes to "MATH", "IO", ... as you like if you don't want to inclute (more or less) everything the library provides to you.

## The other packages
The library itself contains many Low-Level IO and networking classes that may become handy sometimes. But they are overall too specific to explain them here.

# Compatibility
This project is written in Java 8 to allow a exremly wide usability range over the most used versions of Java.

Curently, the most classes will work independently of the underlaying OS, but it is prossible that i later add some OS limited funtionalities.

# Open Source and Licence

[TODO add licence detailes here...]
