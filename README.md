# test-this

Powerful test runner for clojure

`test-this` allows you to run your `clojure.test` tests easily. Some of the features are:

* Fine-grained selection of which namespaces and test functions to run
* **Automatically reload modified files**
* No changes to source code needed, use all your `clojure.test` tests with all its features (including proper handling of
  fixtures)
* Extensible
* Easy to use.

## Usage

### Basic

Add `test-this` as a `dev-dependency`. If you're using leiningen:

    :dev-dependencies [[test-this "0.1.0-SNAPSHOT"]]

If you're using maven, you already know how to do this.

Now, start a REPL and get `test-this` functions

    (use 'test-this)

To run all your tests you can do

    (run-tests)

This will search all tests in your test directory and run them all.

Go code a bit more, modify your code and your tests. Now run `(run-tests)` again on the same
REPL. The files you modified and the ones depending on them will be reloaded, and all
tests will be run again. You don't need to do anything else, `test-this` will reload
only what needs to be reloaded.

### Selecting which test namespaces to run

Maybe you don't want to run all your test namespaces. You can do fine-grained selection
of which namespaces you want to run:

    (run-tests :namespaces ['my-lib.test-foo 'my-lib.test-bar])

Will run only the tests contained in `my-lib.test-foo` and `my-lib.test-bar` namespaces.

You can even run all namespaces matching a regular expression

    (run-tests :namespaces [#".*foo[-\.]bar"])

Or maybe, you can use metadata to identify which namespaces to run:

    (ns ^:wip test.foo)
    (deftest ....)

And now, just do

    (run-tests :namespaces [:wip])

And all your Work In Progress namespaces will be tested.

If you need total control, you can pass a predicate that will receive the namespace

    (run-tests :namespaces [(fn [ns] (= (ns-name ns) "test.foo"))])

All elements of the `:namespaces` vector are ored together. So if you do

    (run-tests :namespaces [:wip 'my-lib.test-foo])

All Work In Progress tests AND tests in `my-lib.test-foo` will be run.

If you need "and" logic, use a vector:

    (run-tests :namespaces [[:wip :integration]])

will run only tests in namespaces with metadata keys `:wip` AND `:integration`.

### Selecting which test functions to run

The same logic applies to selecting test functions

    (deftest ^:wip test-my-foo
      (is true))

    (run-tests :tests [:wip])

Will run only :wip tests in all namespaces. Of course, you can combine everything, use:

    (run-tests :namespaces [[:integration #".*edit.*"]]
               :tests [:wip])

to run tests in integration namespaces matching "edit", and only :wip functions.

### Customization

Further options to run-tests include:

* `:test-dir` is a string with the directory where your tests reside. Default: `"test"`.
* `:reload-dirs` is one or a sequence of directories to watch for changes and reload if needed. Default: `["src" "test"]`.
* `:reload?` if false, no reload will be done, even if source files changed. Default: `true`.
* `:before` is a function called without arguments before executing any tests. Default: noop
* `:after` is a function called with the results map after executing all tests. Default: `identity`.

If you need more control, take a look at the documentation.

## ToDo

* Autotest
* re-run failing tests
* Add clojure.stacktrace
* Add difftest

## License


Copyright (C) 2011 Sebastián B. Galkin

Distributed under the Eclipse Public License, the same as Clojure.
