# SRDB: Scala Relational DataBase Api

[![Build Status](https://travis-ci.org/agilogy/srdb-core.svg?branch=master)](https://travis-ci.org/agilogy/srdb-core)
[![Coverage Status](https://coveralls.io/repos/agilogy/srdb-core/badge.svg)](https://coveralls.io/r/agilogy/srdb-core)

This is a Work In Progress...

## TO-DO
- Documentation
- Make exception translation more powerful
    - Include the sql and arguments in the exception if appropriate
    - Include the reader in the exception if appropriate
- Check the select "template" against Spring JdbcTemplate or something else
- Publish it as open source

## Installation

```
resolvers += Resolver.url("Agilogy Scala",url("http://dl.bintray.com/agilogy/scala/"))(Resolver.ivyStylePatterns)

libraryDependencies += "com.agilogy" %% "srdb-core" % "1.0.0"
```

## Usage

TO-DO

## Copyright

Copyright 2015 Agilogy

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the 
License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an 
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific 
language governing permissions and limitations under the License.