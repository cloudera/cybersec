
This project provides the core Parser Chain API and functionality. This allows individual parsers to be authored and discovered, parser chains to be constructed and executed, and 

* [Parser](#parser)
* [Message](#message)
* [ChainBuilder](#chainbuilder)
* [ChainRunner](#chainrunner)
* [ParserCatalog](#parsercatalog)

### Parser

A `Parser` is responsible for parsing a `Message`.  A parser chain is composed of many parsers that work together to parse a message.

A `Parser` should be annotated using the `MessageParser` annotation. This annotation allows the author to expose parser metadata to the user.  This annotation also makes a parser discoverable using a `ParserCatalog`.

### Message

A `Message` is what a `Parser` parses.  A `Message` is composed of a set of fields. A `Parser` reads from one or more fields of an input message and creates new output fields, modifies existing output fields, or removes output fields.

In many cases a `Parser` simply needs to add an additional field to an existing set of message fields created by other parsers in the chain.  Given an `input` message that operation might look like the following.
```
Message input = ...;

Message output = Message.builder()
    .withFields(input)
    .addField(FieldName.of("output field"), FieldValue.of("output value"))
    .build();
```

If a `Parser` does not pass through fields from the "input" message, those fields have in effect been removed from the parsed message.  

A `Message` can also capture an error that occurred during parsing using either `withError(Throwable)` or `withError(String)`.
```
Message output = Message.builder()
    .withError(new IllegalStateException("this is an error"))
    .build();
```

### ChainBuilder

Provides a fluent API for the construction of a parser chain.  This abstracts and simplifies the construction of parser chains, hiding an implementation which composes many `ChainLink` and `Parser` objects into a single parser chain.  In this sense, it is optional.

Use the methods `then(Parser)` and `then(ChainLink)` to create simple, sequential chains. The method `head()` retrieves a reference to the head of, and in effect the entire, parser chain. 
```
ChainLink chain = new ChainBuilder()
    .then(firstParser)
    .then(secondParser)
    .then(thirdParser)
    .head();
```

Use the methods `routeBy(FieldName)`, `thenMatch(Regex, ChainLink|Parser)`, and `thenDefault(ChainLink|Parser)` to add routing to a parser chain.
```
ChainLink chain = new ChainBuilder()
    .routeBy(FieldName.of("asa_tag"))
    .thenMatch(Regex.of("%ASA-6-302021:"), asa6Parser)
    .thenMatch(Regex.of("%ASA-9-302041:"), asa9Parser)
    .thenDefault(defaultParser)
    .head();
```

For reasonably complex parser chains, multiple 'sub-chains' can be composed using a router in the following manner.
```
ChainLink subChain1 = ...;
ChainLink subChain2 = ...;

ChainLink chain = new ChainBuilder()
    .then(csvParser)
    .routeBy(FieldName.of("asa_tag"))
    .thenMatch(Regex.of("%ASA-6-302021:"), subChain1)
    .thenMatch(Regex.of("%ASA-9-302041:"), subChain2)
    .head();
```

### ChainRunner

Parses a `Message` by executing a parser chain. Returns a list of messages, one `Message` for each `Parser` in the parser chain. 
```
List<Message> results = new ChainRunner()
    .run("one,two,three,four", chain);
```

The last element in the resulting list is the final, fully parsed message.

The first element in the resulting list is the original message passed into the `ChainRunner`.  

### ParserCatalog

Provides a catalog of all parsers available to the user.  The primary implementation of this functionality is the `ClassIndexParserCatalog`.

```
List<ParserInfo> parsers = new ClassIndexParserCatalog().getParsers();
```

The `ParserInfo` provides metadata about each parser like a name, description, and the implementation class.


### ConfigDescriptor

This classes allows a parser author to describe the configuration parameters available to a user. A list of `ConfigDescriptor` values are returned by a parser's `validConfiguration()` method.