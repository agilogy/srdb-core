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
resolvers += "Agilogy GitLab" at "https://gitlab.com/api/v4/groups/583742/packages/maven"

libraryDependencies += "com.agilogy" %% "srdb-core" % "2.0"
```

## Usage

### Imports

Usually, if you already like the default exception translator, only 2 imports are needed:

```
import com.agilogy.srdb._
import com.agilogy.srdb.Srdb._
```

### selecting data

- Example:

	```
	val readEmployees = readSeq{ rs =>
		(rs.getString("name"),rs.getInt("age"))
	}
	val s1 = select("select name, age from employees")(readEmployees)
	val results = s1(conn)
	```

### updating data

- Example:

	```
	val u1 = update("update employees set salary = salary + 500.0")
	val numberOfUpdatedEmployees = u1(conn)
	```

### setting statement parameters

- Using one single function

	```
	val s2 = select("select name, age from employees where age > ? and age < ?")(readEmployees)
	val results = s2(conn, { ps =>
	  ps.setInt(1,25)
	  ps.setInt(2,45)
	})
	```

- Using multiple argument functions

	```
	val results = s2(conn, _.setInt(_,25), _.setInt(_,45))
	```

### Exceptions

Srdb throws, by default, DbException:

```
trait DbException extends RuntimeException {
  val context: Context
  val sql: String
  val causedBy: Option[Throwable]
}

```

Context gives information about what was Srdb doing when the exception was produced. It is a sealed trait that may be:

- GetGeneratedKeys
- ExecuteQuery
- PrepareStatement
- SetArguments
- ReadResultSet
- ReadGeneratedKeys
- SetFetchSize
- ExecuteUpdate

You can provide an exception translator to get the exceptions you prefer thrown. ExceptionTranslator is just a type alias for a function that takes a context, an sql string and a throwable and returns the exception to be thrown by Srdb:

```
type ExceptionTranslator = (Context, String, Throwable) => Exception
```

To use your own exception translator, create an Srdb instance using `withExceptionTranslator`:

```
val mySrdb:Srdb = com.agilogy.srdb.withExceptionTranslator((ctx,sql,exc) => exc)
import mySrdb._
```

## Publishing

To publish this package to Agilogy's Package Registry, set the `GITLAB_DEPLOY_TOKEN` environment variable and then run the following command in sbt:

```
sbt:simple-db> +publish
```

## Copyright

Copyright 2015 Agilogy

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the 
License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an 
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific 
language governing permissions and limitations under the License.
