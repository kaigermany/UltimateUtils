# UltimateUtils
This is a collection of classes and methods that you might need from time to time.
It's loosely based on my old library, but a fundamental redesign of it.

Note that the license is not yet set.

# Features
- JSON. just JSON.
- DDS Image reading and writing.
- DNS Resolver: Allows you to set a custom DNS resolver for the whole runtime instance.
- HttpServer: Need a mini testserver? Here it is.
- SmartHTTP: A new implementation of the HTTP protocol. It also makes active use of the connection-keepalive hint.
- WebSocketServer: If you need to connect a Browser to your program, you can use this class.
- WebSocket: ...or instead connect to a remote WebSocket Server?
- Parallel.exec(): A simple way to spread work over all CPU cores.
- ThreadLock: A good helper for flow-control between your Threads.
- ClassExtensions: Include it into your class to run smaller projects, tests or such.
Alternatively, you can just include "MATH", "IO", ... explicitly if you don't want to include everything else.

# Compatibility
This project is Java 8 compatible to allow a exremly wide usability range over the most used versions of Java.

Currently, the most classes will work independently of the underlaying OS, but it is prossible that i later add some OS limited funtionalities.

# Open Source and Licence

[TODO add licence detailes here...]
