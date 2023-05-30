package org.spellchecker;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.ListItem;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;
import org.spellchecker.model.SourceMap;
import org.spellchecker.model.Issue;
import org.jsoup.Jsoup;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.spelling.morfologik.MorfologikSpellerRule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AsciidocIssueFinder {

    private final JLanguageTool langTool;

    private final Asciidoctor asciidoctor;

    private List<Issue> issues;

    public AsciidocIssueFinder(String locale, List<String> wordsIgnored) {
        langTool = new JLanguageTool(Languages.getLanguageForShortCode(locale));
        for (Rule rule : langTool.getAllActiveRules()) {
            if (rule.getId().equals("MORFOLOGIK_RULE_"+locale.toUpperCase().replace("-", "_"))) {
                ((MorfologikSpellerRule) rule).addIgnoreTokens(wordsIgnored);
            }
        }

        asciidoctor = Asciidoctor.Factory.create();
    }

    public List<Issue> parseAsciidoc(String file, String path) throws IOException {
        Options options = Options
                .builder()
                .safe(SafeMode.SAFE)

                .sourceDir(new File(path))
                .baseDir(new File(path))
                .sourcemap(true)
                .build();
        var document = asciidoctor.load(Files.readString(Path.of(file)), options);
        issues = new ArrayList<>();
        process(document);
        System.out.println("Analysis found " + issues.size() + " issues");
        return issues;
    }

    private void process(StructuralNode block) throws IOException {
        List<StructuralNode> blocks = block.getBlocks();

        for (final StructuralNode currentBlock : blocks) {
            checkBlock(currentBlock);
        }
    }

    private void checkBlock(StructuralNode currentBlock) throws IOException {
        if (currentBlock != null) {
            SourceMap sourceMap = null;
            if (currentBlock instanceof ListItem list) {
                sourceMap = new SourceMap(list.getText(), list.getSourceLocation().getLineNumber(), list.getSourceLocation().getPath());
            } else
            if (currentBlock instanceof Section section) {
                sourceMap = new SourceMap(section.getTitle(), section.getSourceLocation().getLineNumber(), section.getSourceLocation().getPath());

            } else
            if (currentBlock.getContent() instanceof String content) {
                sourceMap = new SourceMap(content, currentBlock.getSourceLocation().getLineNumber(), currentBlock.getSourceLocation().getPath());

            }

            if (sourceMap != null && sourceMap.getText() != null && !sourceMap.getText().isEmpty()) {
                List<String> rulesToIgnore = extractRuleToIgnore(currentBlock);
                processSourceMap(sourceMap);

                List<RuleMatch> matches = langTool.check(sourceMap.getText());

                for (RuleMatch match : matches) {
                    handleRules(sourceMap, rulesToIgnore, match);
                }
            }

            if ((currentBlock.getBlocks() != null && !currentBlock.getBlocks().isEmpty())) {
                process(currentBlock);
            }
        }
    }

    private List<String> extractRuleToIgnore(StructuralNode currentBlock) {
        List<String> toReturn = new ArrayList<>();
        var ignoreAttribute = currentBlock.getAttribute("ignore");
        if (ignoreAttribute instanceof String rules) {
            toReturn.addAll(List.of(rules.split(" ")));
        }
        return toReturn;
    }

    private void handleRules(SourceMap sourceMap, List<String> rulesToIgnore, RuleMatch match) {
        if (rulesToIgnore.contains(match.getRule().getId())) {
            return;
        }
        System.out.println("Potential error in file " + sourceMap.getSourceFile() + " on line " + sourceMap.getSourceLine() + " at characters " +
                match.getFromPos() + "-" + match.getToPos() + " " + sourceMap.getText().substring(match.getFromPos(), match.getToPos()) + ": " +
                match.getMessage());
        System.out.println("Rule ID: " +
                match.getRule().getId());
        System.out.println("Rule DESC: " +
                match.getRule().getDescription());
        System.out.println("Suggested correction(s): " +
                match.getSuggestedReplacements());
        System.out.println("---");
        issues.add(new Issue(match, sourceMap));
    }

    private void processSourceMap(SourceMap sourceMap) {
        sourceMap.setText(Jsoup.parse(sourceMap.getText()).text());
    }
}