# itHub

**itHub** is a Spring Boot Java web application. Its frontend layouts are very similar to GitHub's. I focused on the version controlling feature, so you can find the approximate logic about the way git creates git objects from [here](./src/main/java/com/project/myApplication/util/).

My primary goals were:  
- Build an application with Spring Boot
- Handle HTTP multi-part file uploads
- Process files Using file I/O methods

## Installation and Getting Started
---

Prerequisites:
- JDK 11 
- H2 Database Engine Version 1.4.200

  
We need to configure database first. Please follow directions below.  
&nbsp; 1. Open H2 console and create a new database from **[this](./sql/ddl.sql)** script.  
&nbsp; 2. Check **application.properties** file if you want to define other connection attributes.  

😭 user whose information is set in [this](src\main\java\com\project\myApplication\WebSecurityConfig.java) file can only access his/her repositories. It's totally not secure. Sorry for your inconvenience.