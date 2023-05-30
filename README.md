# Asciidoc Spell Checker
A simple Java application using jAsciidoctor and Language Tool to produce a SARIF json file.

## Usage
### Using docker
`docker run XXXX`
### Env variable
Set the following env variable
* `ASCIIDOC_LANG` the language code of the document
* `ASCIIDOC_PATH` the base path
* `ASCIIDOC_FILE` the file path to analyze
* `ASCIIDOC_WORDS_IGNORED` the file path containing the words to ignore (optional)
* `ASCIIDOC_SARIF_FILE` the path of the sarif output

For example:
```
ASCIIDOC_LANG=it-IT
ASCIIDOC_PATH=/documents
ASCIIDOC_FILE=/documents/book.adoc
ASCIIDOC_WORDS_IGNORED=/documents/wordsIgnored.txt
ASCIIDOC_SARIF_FILE=/documents/book.sarif.json
```

## Rule manipulation
### Ignored words
You can add word to the dictionary creating a file with one word per line and use it as value in the `ASCIIDOC_WORDS_IGNORED`

### Ignored rules
You can exclude rules from a paragraph/section using a custom attribute `ignore`:

```
[ignore="RULE_01_002 ANOTHER_03_001"]
The rules in ignore will not trigger
```

## Disclaimer
This program was created in a hurry, has no testing, coverage and use a build and fix approach.

Every contribution in the form of Issue or Merge Request is welcome. 

Use it in production at your own risk!
