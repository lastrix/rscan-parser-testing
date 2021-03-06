/*
 * Copyright (C) 2019-2020.  rscan-parser-testing project
 *
 * This file is part of rscan-parser-testing project.
 *
 * rscan-parser-testing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * rscan-parser-testing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with rscan-parser-testing.  If not, see <http://www.gnu.org/licenses/>.
 */

package antlr4.patches

import antlr4.GrammarPatches

class RScanPatches {
    private static final String PATH = "shared/grammar_patches.json"

    static void main(String[] args) throws IOException {
        getGrammarPatches().save(new File(PATH.replace('/', File.separator)))
    }

    static GrammarPatches getGrammarPatches() {
        return new GrammarPatches()

                .parser(
                        "\\Qstatic { RuntimeMetaData.checkVersion(\"\\E\\d(\\.\\d)+\\Q\", RuntimeMetaData.VERSION); }\\E",
                        ""
                )
                .parser(
                        "\\Qprotected static final DFA[] _decisionToDFA;\\E\\s+" +
                                "\\Qprotected static final PredictionContextCache _sharedContextCache =\\E\\s+" +
                                "\\Qnew PredictionContextCache();\\E",
                        ""
                )
                .parser(
                        "\\Q extends Parser \\E",
                        " extends Abstract##{grammarName}## "
                )

                .parser(
                        "\\QParser(TokenStream input) {\\E\\s+\\Qsuper(input);\\E",
                        "Parser(@NotNull TokenStream input, @NotNull VirtualFile file, ATN atn, DFA[] decisionToDFA) {\n" +
                                "\t\tsuper(input, file);"
                )

                .parser(
                        "\\Q/**\\E\\s+" +
                                "\\Q* @deprecated Use {@link #VOCABULARY} instead.\\E\\s+" +
                                "\\Q*/\\E\\s+" +
                                "\\Q@Deprecated\\E\\s+" +
                                "\\Qpublic static final String[] tokenNames;\\E\\s+" +
                                "\\Qstatic {\\E\\s+" +
                                "\\QtokenNames = new String[_SYMBOLIC_NAMES.length];\\E\\s+" +
                                "\\Qfor (int i = 0; i < tokenNames.length; i++) {\\E\\s+" +
                                "\\QtokenNames[i] = VOCABULARY.getLiteralName(i);\\E\\s+" +
                                "\\Qif (tokenNames[i] == null) {\\E\\s+" +
                                "\\QtokenNames[i] = VOCABULARY.getSymbolicName(i);\\E\\s+" +
                                "}\\s+" +
                                "\\Qif (tokenNames[i] == null) {\\E\\s+" +
                                "\\QtokenNames[i] = \"<INVALID>\";\\E\\s+" +
                                "}\\s+" +
                                "}\\s+" +
                                "}\\s+" +
                                "\\Q@Override\\E\\s+" +
                                "\\Q@Deprecated\\E\\s+" +
                                "\\Qpublic String[] getTokenNames() {\\E\\s+" +
                                "\\Qreturn tokenNames;\\E\\s+" +
                                "}",
                        ""
                )

                .parser(
                        "\\Q_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);\\E",
                        "_ATN = atn; _interp = new ParserATNSimulator(this,atn,decisionToDFA,new PredictionContextCache());"
                )
                .parser(
                        "\\Qpublic static final ATN _ATN =\\E\\s+" +
                                "\\Qnew ATNDeserializer().deserialize(_serializedATN.toCharArray());\\E\\s+" +
                                "\\Qstatic {\\E\\s+" +
                                "\\Q_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];\\E\\s+" +
                                "\\Qfor (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {\\E\\s+" +
                                "\\Q_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);\\E\\s+" +
                                "\\Q}\\E\\s+" +
                                "\\Q}\\E",
                        "public final ATN _ATN;"
                )

        /////////////////////////////////
        ///// Lexer patches ////////////
        ///////////////////////////////
                .lexer(
                        "\\Qextends Lexer \\E",
                        "extends Abstract##{grammarName}## "
                )

                .lexer(
                        "\\Qstatic { RuntimeMetaData.checkVersion(\"\\E\\d(\\.\\d)+\\Q\", RuntimeMetaData.VERSION); }\\E",
                        "")

                .lexer(
                        "\\Qprotected static final DFA[] _decisionToDFA;\\E\\s+" +
                                "\\Qprotected static final PredictionContextCache _sharedContextCache =\\E\\s+" +
                                "\\Qnew PredictionContextCache();\\E",
                        ""
                )

                .lexer(
                        "\\Q_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);\\E",
                        "_ATN = atn; _interp = new LexerATNSimulator(this,atn,decisionToDFA,new PredictionContextCache());"
                )

                .lexer(
                        "\\Q/**\\E\\s+" +
                                "\\Q* @deprecated Use {@link #VOCABULARY} instead.\\E\\s+" +
                                "\\Q*/\\E\\s+" +
                                "\\Q@Deprecated\\E\\s+" +
                                "\\Qpublic static final String[] tokenNames;\\E\\s+" +
                                "\\Qstatic {\\E\\s+" +
                                "\\QtokenNames = new String[_SYMBOLIC_NAMES.length];\\E\\s+" +
                                "\\Qfor (int i = 0; i < tokenNames.length; i++) {\\E\\s+" +
                                "\\QtokenNames[i] = VOCABULARY.getLiteralName(i);\\E\\s+" +
                                "\\Qif (tokenNames[i] == null) {\\E\\s+" +
                                "\\QtokenNames[i] = VOCABULARY.getSymbolicName(i);\\E\\s+" +
                                "}\\s+" +
                                "\\Qif (tokenNames[i] == null) {\\E\\s+" +
                                "\\QtokenNames[i] = \"<INVALID>\";\\E\\s+" +
                                "}\\s+" +
                                "}\\s+" +
                                "}\\s+" +
                                "\\Q@Override\\E\\s+" +
                                "\\Q@Deprecated\\E\\s+" +
                                "\\Qpublic String[] getTokenNames() {\\E\\s+" +
                                "\\Qreturn tokenNames;\\E\\s+" +
                                "}",
                        ""
                )
                .lexer(
                        "\\Qpublic static final ATN _ATN =\\E\\s+" +
                                "\\Qnew ATNDeserializer().deserialize(_serializedATN.toCharArray());\\E\\s+" +
                                "\\Qstatic {\\E\\s+" +
                                "\\Q_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];\\E\\s+" +
                                "\\Qfor (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {\\E\\s+" +
                                "\\Q_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);\\E\\s+" +
                                "\\Q}\\E\\s+" +
                                "\\Q}\\E",
                        "public final ATN _ATN;"
                )
                .lexer(
                        "\\QLexer(CharStream input) {\\E\\s+\\Qsuper(input);\\E",
                        "Lexer(@NotNull CharStream input, @NotNull VirtualFile file, ATN atn, DFA[] decisionToDFA) {\n" +
                                "\t\tsuper(input, file);"
                )
    }
}
