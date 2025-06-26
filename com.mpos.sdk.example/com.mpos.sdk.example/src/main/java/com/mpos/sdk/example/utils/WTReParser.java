package com.mpos.sdk.example.utils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Alex Skalozub
 * https://github.com/pieceofsummer/WTReTextField
 */
public class WTReParser {

    public class NSRange {
        public Integer location;
        public Integer length;

        public NSRange(int loc, int len) {
            location = loc;
            length = len;
        }
    }

    public class WTReNode {
        public NSRange sourceRange;

        public WTReNode parent;
        public WTReNode nextSibling;

        public String displayString(String pattern) {
            return pattern.substring(sourceRange.location, sourceRange.location + sourceRange.length);
        }
    }

    public class WTReGroup extends WTReNode {
        boolean capturing;

        ArrayList<WTReNode> children;

        @Override
        public String displayString(String pattern) {
            return "(" + super.displayString(pattern) + ")";
        }

        public WTReGroup() {
            children = new ArrayList<WTReNode>();
        }
    }

    public class WTReAlteration extends WTReNode {
        ArrayList<WTReNode> children;

        public WTReAlteration() {
            children = new ArrayList<WTReNode>();
        }
    }

    public class WTReQuantifier extends WTReNode {
        public WTReNode child;
        public boolean greedy;
        int countFrom;
        int countTo;

        public WTReQuantifier() {
            greedy = true;
            countFrom = 1;
            countTo = 1;
        }

        public WTReQuantifier(int from, int to) {
            greedy = true;
            countFrom = from;
            countTo = to;
        }

        public String displayQuantifier() {
            String pat;

            if (countFrom == 0) {
                if (countTo == Integer.MAX_VALUE) {
                    pat = "*";
                } else if (countTo == 1) {
                    pat = "?";
                } else {
                    pat = String.format("{,%d}", countTo); //{,%u} !
                }
            } else if (countFrom == 1 && countTo == Integer.MAX_VALUE) {
                pat = "+";
            } else if (countFrom == countTo) {
                pat = String.format("{%d}", countFrom);
            } else if (countTo == Integer.MAX_VALUE) {
                pat = String.format("{%d,}", countFrom);
            } else {
                pat = String.format("{%d,%d}", countFrom, countTo);
            }
            if (!greedy) pat += "?";

            return pat;

        }

        @Override
        public String displayString(String pattern) {
            return child.displayString(pattern) + displayQuantifier();
        }
    }

    public class WTReCharacterBase extends WTReNode {
        boolean ignoreCase;

        public boolean matchesCharacter(char c) throws Exception {
            throw new Exception("Invalid operation!");
        }
    }

    public class WTReCharacterSet extends WTReCharacterBase {
        public boolean negation;
        public String chars;


        @Override
        public boolean matchesCharacter(char c) {
            boolean contains = Character.toString(c).matches(chars);
/*
            boolean contains = chars.indexOf(c) != -1;
            if (!contains && ignoreCase) {
                contains = chars.toLowerCase().indexOf(Character.toLowerCase(c)) != -1;
            }
           */

            return contains ^ negation;
        }

        @Override
        public String displayString(String pattern) {
            return "[" + super.displayString(pattern) + "]";
        }
    }

    public class WTReLiteral extends WTReCharacterBase {
        char character;

        @Override
        public boolean matchesCharacter(char c) {
            boolean contains = character == c;
            if (!contains && ignoreCase) {
                contains = Character.toLowerCase(c) == Character.toLowerCase(character);
            }

            return contains;
        }

        @Override
        public String displayString(String pattern) {
            return "'" + character + "'";
        }
    }

    public class WTReAnyCharacter extends WTReCharacterBase {
        @Override
        public boolean matchesCharacter(char c) {
            return true;
        }

        @Override
        public String displayString(String pattern) {
            return ".";
        }
    }

    public class WTReEndOfString extends WTReCharacterBase {
        @Override
        public String displayString(String pattern) {
            return "$";
        }

        @Override
        public boolean matchesCharacter(char c) {
            return c == 0;
        }
    }

    public class WTState {
        public ArrayList<WTTransition> transitions;
        public boolean isFinal;

        public WTState() {
            transitions = new ArrayList<WTTransition>();
        }
    }

    public class WTTransition {
        public WTReCharacterBase node;
        public WTReLiteral bypassNode;
        public WTState nextState;
    }


    //Variables
    String _pattern;
    boolean _ignoreCase;
    WTReGroup node;
    boolean _finished;
    Pattern _exactQuantifierRegex;
    Pattern _rangeQuantifierRegex;

    public WTReParser(String pattern) throws Exception {
        this(pattern, false);
    }

    public WTReParser(String pattern, boolean ignoreCase) throws Exception {
        _pattern = pattern;
        _ignoreCase = ignoreCase;
        node = null;
        this.parsePattern();
    }

    public void parsePattern() throws Exception {
        if (node != null) return;

        if (!_pattern.startsWith("^")) throw new Exception("Invalid pattern start");

        _finished = false;

        _exactQuantifierRegex = Pattern.compile("^\\{\\s*(\\d+)\\s*\\}$");
        _rangeQuantifierRegex = Pattern.compile("^\\{\\s*(\\d*)\\s*,\\s*(\\d*)\\s*\\}$");

        node = parseSubpattern(_pattern, new NSRange(1, _pattern.length() - 1), false);

        _exactQuantifierRegex = null;
        _rangeQuantifierRegex = null;

        if (!_finished) throw new Exception("Invalid pattern end");
    }

    boolean isValidEscapedChar(char c, boolean inCharset) {
        switch (c) {
            case '(':
            case ')':
            case '[':
            case ']':
            case '{':
            case '}':
            case '\\':
            case '|':
            case 'd':
            case 'D':
            case 'w':
            case 'W':
            case 's':
            case 'S':
            case 'u':
            case '\'':
            case '.':
            case '+':
            case '*':
            case '?':
            case '$':
            case '^':
                return true;

            case '-':
                return inCharset;

            default:
                return false;
        }
    }

    WTReCharacterBase parseCharset(String pattern, NSRange range, boolean enclosed) throws Exception {
        boolean negation = false;
        int count = 0;
        char lastChar = 0;
        String chars = "";
        boolean escape = false;

        for (int i = 0; i < range.length; i++) {
            char c = pattern.charAt(range.location + i);

            String strRegex = "";

            if (enclosed && i == 0 && c == '^') {
                negation = true;
                continue;
            }

            if (c == '\\' && !escape) {
                escape = true;
                continue;
            }

            if (escape) {
                // process character classes and escaped special chars
                if (!this.isValidEscapedChar(c, enclosed)) throw new Exception("isValidEscapedChar");

                if (c == 'd') {
                    strRegex = "\\d";
                    count += 2;
                } else if (c == 'D') {
                    strRegex = "\\D";
                    count += 2;
                } else if (c == 'w') {
                    strRegex = "\\w";
                    count += 2;
                } else if (c == 'W') {
                    strRegex = "\\W";
                    count += 2;
                } else if (c == 's') {
                    strRegex = "\\s";
                    count += 2;
                } else if (c == 'S') {
                    strRegex = "\\S";
                    count += 2;
                } else if (c == 'u') {
                    // unicode character in format \uFFFF

                    if (i + 4 >= range.length) throw new Exception("Expected a four-digit hexadecimal character code");
                    String str = _pattern.substring(range.location = i + 1, range.location = i + 1 + 4);
                    strRegex = "\\u" + str;
                    i += 4;
                    count++;
                } else {
                    // todo: check for other escape sequences

                    lastChar = c;
                    chars += c;
                    count++;
                    // [chars addCharactersInRange:NSMakeRange(c, 1)];
                    // lastChar = c;
                    // count++;
                }

                escape = false;
            } else if (enclosed && c == '-' && i > 0 && i < range.length - 1) {
                // process character range

                char rangeStart = _pattern.charAt(range.location + i - 1);
                char rangeEnd = _pattern.charAt(range.location + i + 1);

                if (rangeEnd < rangeStart) throw new Exception("Invalid character range");

                strRegex = "[" + rangeStart + "-" + rangeEnd + "]";

                i++;
                count += 2;
            } else {
                strRegex = Character.toString(c);
                lastChar = c;
                count++;
            }

            chars += strRegex + "|";
        }

        if (!negation && count == 1) {
            WTReLiteral l = new WTReLiteral();
            l.character = lastChar;
            l.sourceRange = range;
            l.ignoreCase = _ignoreCase;
            return l;
        } else {
            WTReCharacterSet s = new WTReCharacterSet();
            s.negation = negation;
            if (chars.charAt(chars.length() - 1) == '|')
                chars = chars.substring(0, chars.length()-1);
            s.chars = chars;
            s.sourceRange = range;
            s.ignoreCase = _ignoreCase;
            return s;
        }
    }

    public WTReGroup groupFromNodes(ArrayList<WTReNode> nodes, boolean enclosed) {
        if ((nodes.size() == 1) && (nodes.get(0) instanceof WTReGroup)) {
            WTReGroup t = (WTReGroup) nodes.get(0);
            if (t instanceof WTReGroup) {
                t.capturing |= enclosed;
                return t;
            }
        }

        WTReGroup g = new WTReGroup();

        g.children = (ArrayList<WTReNode>)nodes.clone();
        g.capturing = enclosed;

        // setup links
        WTReNode prev = g.children.get(0);

        prev.parent = g;

        for (int i = 1; i < g.children.size(); i++) {
            WTReNode curr = g.children.get(i);
            curr.parent = g;
            prev.nextSibling = curr;
            prev = curr;
        }

        return g;
    }

    WTReGroup parseSubpattern(String pattern, NSRange range, boolean enclosed) throws Exception {

        ArrayList<WTReNode> nodes = new ArrayList<WTReNode>(range.length);

        ArrayList<WTReNode> alternations = null;
        int startPos = 0, endPos = range.length;

        boolean escape = false;
        WTReNode lastnode = null;

        for (int i = 0; i < range.length; i++) {
            if (_finished) throw new Exception("Found pattern end in the middle of string");

            char c = pattern.charAt(range.location + i);

            if (enclosed && i == 0 && c == '?') {
                // group modifiers are present

                if (range.length < 3) throw new Exception("Invalid group found in pattern");


                char d = pattern.charAt(range.location + i + 1);
                if (d == '<') {
                    // tagged group (?<style1>…)
                    for (int j = i + 2; j < range.length; j++) {
                        d = pattern.charAt(range.location + j);
                        ;

                        if (d == '<') {
                            throw new Exception("Invalid group tag found in pattern");
                        } else if (d == '>') {
                            if (j == i + 2) throw new Exception("Empty group tag found in pattern");
                            i = j;
                            break;

                        } else if (Character.isLetterOrDigit(d)) {
                            throw new Exception("Group tag contains invalid chars");
                        }
                    }
                } else if (d == '\'') {
                    // tagged group (?'style2'…)
                    for (int j = i + 2; j < range.length; j++) {
                        d = pattern.charAt(range.location + j);

                        if (d == '\'') {
                            if (j == i + 2) throw new Exception("Empty group tag found in pattern");
                            i = j;
                            break;
                        } else if (!Character.isLetterOrDigit(d)) {
                            throw new Exception("Group tag contains invalid chars");
                        }
                    }
                } else if (d == ':') {
                    // non-capturing group
                    enclosed = false;
                    i++;
                } else {
                    throw new Exception("Unknown group modifier");
                }

                continue;
            }

            if (c == '\\' && !escape) {
                escape = true;
                continue;
            }

            if (escape) {
                if (!isValidEscapedChar(c, false) || i == 0) throw new Exception("Invalid escape sequence");

                lastnode = this.parseCharset(pattern, new NSRange(range.location + i - 1, 2), false);
                nodes.add(lastnode);

                escape = false;
            } else if (c == '(') {
                int brackets = 1;
                boolean escape2 = true;

                for (int j = i + 1; j < range.length; j++) {
                    char d = pattern.charAt(range.location + j);

                    if (escape2) {
                        escape2 = false;
                    } else if (d == '\\') {
                        escape2 = true;
                    } else if (d == '(') {
                        brackets++;
                    } else if (d == ')') {
                        brackets--;

                        if (brackets == 0) {
                            lastnode = this.parseSubpattern(pattern, new NSRange(range.location + i + 1, j - i - 1), true);
                            nodes.add(lastnode);
                            i = j;
                            break;
                        }
                    }
                }

                if (brackets != 0) throw new Exception("Unclosed group bracket");
            } else if (c == ')') {
                throw new Exception("Unopened group bracket");
            } else if (c == '[') {
                boolean escape2 = false;
                boolean valid = false;

                for (int j = i + 1; j < range.length; j++) {
                    char d = pattern.charAt(range.location + j);

                    if (escape2) {
                        escape2 = false;
                    } else if (d == '\\') {
                        escape2 = true;
                    } else if (d == '[' || d == '(' || d == ')') {
                        // invalid character
                        break;
                    } else if (d == ']') {
                        lastnode = this.parseCharset(pattern, new NSRange(range.location + i + 1, j - i - 1), true);
                        nodes.add(lastnode);

                        i = j;
                        valid = true;
                        break;
                    }
                }

                if (!valid) throw new Exception("Unclosed character set bracket");
            } else if (c == ']') {
                throw new Exception("Unopened character set bracket");
            } else if (c == '{') {
                if (lastnode == null || lastnode instanceof WTReQuantifier) throw new Exception("Invalid quantifier usage");

                boolean valid = false;

                for (int j = i + 1; j < range.length; j++) {
                    char d = pattern.charAt(range.location + j);

                    if (d == '}') {
                        String from, to;

                        String str = pattern.substring(range.location + i, range.location + j + 1);
                        Matcher m = _exactQuantifierRegex.matcher(str);

                        if (m.matches()) {
                            from = m.group(1);
                            to = from;
                        } else {
                            m = _rangeQuantifierRegex.matcher(str);
                            if (!m.matches()) {
                                throw new Exception("Invalid quantifier format");
                            } else {
                                from = m.group(1);
                                to = m.group(2);
                            }
                        }

                        WTReQuantifier qtf = new WTReQuantifier();

                        if (from == null || from.equals("")) qtf.countFrom = 0;
                        else qtf.countFrom = Integer.parseInt(from);

                        if (to == null || to.equals("")) qtf.countTo = Integer.MAX_VALUE;
                        else qtf.countTo = Integer.parseInt(to);

                        if (qtf.countFrom > qtf.countTo) throw new Exception("Invalid quantifier range");

                        nodes.remove(nodes.size() - 1); //removeLastObject
                        qtf.child = lastnode;
                        lastnode.parent = qtf;
                        lastnode = qtf;
                        nodes.add(lastnode);

                        i = j;
                        valid = true;
                        break;
                    }
                }

                if (!valid) throw new Exception("Unclosed quantifier bracket");
            } else if (c == '}') {
                throw new Exception("Unopened qualifier bracket");
            } else if (c == '*') {
                if (lastnode == null || lastnode instanceof WTReQuantifier) throw new Exception("Invalid quantifier usage");

                nodes.remove(nodes.size() - 1);
                WTReQuantifier qtf = new WTReQuantifier(0, Integer.MAX_VALUE);
                qtf.child = lastnode;
                lastnode.parent = qtf;
                lastnode = qtf;
                nodes.add(lastnode);
            } else if (c == '+') {
                if (lastnode == null || lastnode instanceof WTReQuantifier) throw new Exception("Invalid quantifier usage");

                nodes.remove(nodes.size() - 1);
                WTReQuantifier qtf = new WTReQuantifier(1, Integer.MAX_VALUE);
                qtf.child = lastnode;
                lastnode.parent = qtf;
                lastnode = qtf;
                nodes.add(lastnode);
            } else if (c == '?') {
                if (lastnode == null) throw new Exception("Invalid quantifier usage");

                if (lastnode instanceof WTReQuantifier) {
                    ((WTReQuantifier) lastnode).greedy = false;
                } else {
                    nodes.remove(nodes.size() - 1);
                    WTReQuantifier qtf = new WTReQuantifier(0, 1);
                    qtf.child = lastnode;
                    lastnode.parent = qtf;
                    lastnode = qtf;
                    nodes.add(lastnode);
                }

                lastnode = null;
            } else if (c == '.') {
                // any character
                lastnode = new WTReAnyCharacter();
                nodes.add(lastnode);
            } else if (c == '|') {
                // alternation
                if (alternations == null) alternations = new ArrayList<WTReNode>(2);

                WTReGroup g = groupFromNodes(nodes, enclosed);

                g.sourceRange = new NSRange(range.location + startPos, i - startPos);
                startPos = i + 1;

                alternations.add(g);
                nodes.clear();
                lastnode = null;
            } else if (c == '$') {
                if (alternations != null && enclosed) throw new Exception("End of string shouldn't be inside alternation");

                if (range.location + i + 1 < pattern.length()) throw new Exception("Unexpected end of string");

                lastnode = new WTReEndOfString();
                nodes.add(lastnode);

                endPos = i + 1;
                _finished = true;
                break;
            } else {
                lastnode = this.parseCharset(pattern, new NSRange(range.location + i, 1), false);
                nodes.add(lastnode);
            }
        }

        if (escape) throw new Exception("Invalid group ending");

        WTReGroup g = groupFromNodes(nodes, enclosed);
        g.sourceRange = new NSRange(range.location + startPos, endPos - startPos);
        g.capturing = enclosed;

        if (alternations != null) {
            // build alternation and enclose it into group
            alternations.add(g);

            WTReAlteration a = new WTReAlteration();
            a.children = alternations;
            a.sourceRange = new NSRange(range.location, endPos);

            // setup links
            WTReNode prev = alternations.get(0);
            prev.parent = a;

            for (int i = 1; i < alternations.size(); i++) {
                WTReNode curr = alternations.get(i);
                curr.parent = a;
                prev.nextSibling = curr;
                prev = curr;
            }

            g = new WTReGroup();
            g.children = new ArrayList<WTReNode>();
            g.children.add(a);
            g.capturing = enclosed;
            g.sourceRange = a.sourceRange;

            a.parent = g;
        }
        return g;
    }

    public String reformatString(String input) throws Exception
    {

        StringBuilder builder = new StringBuilder(input);
        // empty strings are ok
        if (input == null || input.equals("")) return input;


        WTState initialState = new WTState();
        WTState finalState = processNode(node, initialState, input.length());

        WTState x = stateDSF(initialState, finalState, builder, 0);

        if (x == null)
            return null;
        return x.isFinal ? builder.toString() : null;
    }

    WTState processNode(WTReNode node, WTState state, int length)
    {
        if (node instanceof WTReEndOfString)
        {
            WTState finalState = new WTState();;
            finalState.isFinal = true;

            WTTransition tran = new WTTransition();
            tran.node = (WTReCharacterBase)node;
            tran.nextState = finalState;
            state.transitions.add(tran);

            return finalState;
        }
        else if (node instanceof WTReCharacterBase)
        {
            WTState finalState = new WTState();

            WTTransition tran = new WTTransition();;
            tran.node = (WTReCharacterBase)node;
            tran.nextState = finalState;
            state.transitions.add(tran);

            return finalState;
        }
        else if (node instanceof WTReQuantifier)
        {
            WTReQuantifier qtf = (WTReQuantifier)node;

            WTState curState = state;
            for (int i = 0; i < qtf.countFrom; i++) {
                curState = processNode(qtf.child, curState, length);
            }

            if (qtf.countTo == qtf.countFrom)
            {
                // strict quantifier
                return curState;
            }

            WTState finalState = new WTState();;

            for (int i = qtf.countFrom; i < Math.min(qtf.countTo, length); i++) {
                WTState nextState = processNode(qtf.child, curState, length);

                WTTransition tran = new WTTransition();
                tran.node = null;
                tran.nextState = finalState;

                if (qtf.greedy)
                    curState.transitions.add(tran);
                else
                    curState.transitions.add(0, tran);

                curState = nextState;
            }

            WTTransition tran = new WTTransition();
            tran.node = null;
            tran.nextState = finalState;
            curState.transitions.add(tran);

            return finalState;
        }
        else if (node instanceof WTReGroup)
        {
            WTReGroup grp = (WTReGroup)node;

            WTState curState = state;
            for (int i = 0; i < grp.children.size(); i++) {
                curState = processNode(grp.children.get(i), curState,length);
            }

            if (!grp.capturing && grp.children.size() == 1 && (grp.children.get(0) instanceof WTReLiteral))
            {
                WTTransition tran = new WTTransition();
                tran.node = null;
                tran.bypassNode = (WTReLiteral)grp.children.get(0);
                tran.nextState = curState;
                state.transitions.add(tran);
            }

            return curState;
        }
        else if (node instanceof WTReAlteration)
        {
            WTReAlteration alt = (WTReAlteration)node;

            WTState finalState = new WTState();

            for (int i = 0; i < alt.children.size(); i++) {
                WTState curState = processNode(alt.children.get(i),state, length);

                WTTransition tran = new WTTransition();
                tran.node = null;
                tran.nextState = finalState;
                curState.transitions.add(tran);
            }

            return finalState;
        }
        else
        {
            return null;
        }
    }


    WTState stateDSF(WTState initial, WTState fin, StringBuilder input, int startPos) throws Exception {
//        {
//            StringBuilder tmp = new StringBuilder(input.toString());
//            nextState(initial, fin, tmp, startPos);
//            tmp.toString();
//        }
//
        ArrayList<PathEntry> path = new ArrayList<PathEntry>();

        path.add(new PathEntry(initial, startPos));

        while (path.size() > 0) {
            int depth = path.size() - 1;

            WTState parent = path.get(depth).getState();
            int vertexNum = path.get(depth).getVertex();
            int position = path.get(depth).getPosition();

            if (parent != null && parent.isFinal) {
                try {
                    for (int i = path.size() - 1; i > 0; i --) {
                        WTTransition tran = path.get(i - 1).getState().transitions.get(path.get(i - 1).getVertex() - 1);
                        if (tran.bypassNode != null)
                            input.insert(path.get(i).getPosition(), Character.toString(tran.bypassNode.character));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return parent;
            }

            if (position > input.length()) {
                path.get(depth).setVertex(vertexNum + 1);
                path.add(new PathEntry(fin));
                continue;
            }

            if (parent != null && parent.transitions.size() > vertexNum) {
                WTTransition nextTransition = parent.transitions.get(vertexNum);

                if (nextTransition.node != null) {
                    char c = (position < input.length()) ? input.charAt(position) : 0;
                    if (!nextTransition.node.matchesCharacter(c)) {
                        if (c == 0) {
                            path.get(depth).setVertex(vertexNum + 1);
                            path.add(new PathEntry(fin));
                            continue;
                        } else {
                            path.get(depth).setVertex(vertexNum + 1);
                            continue;
                        }
                    } else {

                    }
                    position ++;
                }

                path.get(depth).setVertex(vertexNum + 1);
                path.add(new PathEntry(nextTransition.nextState, position));
            } else {
                path.remove(depth);
            }
        }
        return null;
    }

    WTState nextState(WTState state, WTState fin, StringBuilder input, int pos) throws Exception
    {
        if (state.isFinal) return state;

        if (pos > input.length()) return fin;

        for (WTTransition tran : state.transitions) {

            int nextPos = pos;

            if (tran.node != null) {
                char c = (pos < input.length()) ? input.charAt(pos) : 0;
                if (!tran.node.matchesCharacter(c))
                {
                    if (c == 0) return fin;
                    continue;
                }
                else
                {

                }
                nextPos += 1;
            }

            WTState s = nextState(tran.nextState, fin, input, nextPos);
            if (s != null && s.isFinal)
            {
                if (tran.bypassNode != null)

                    input.insert(nextPos, Character.toString(tran.bypassNode.character));

                return s;
            }
        }

        return null;
    }

    private class PathEntry {

        private final WTState state;
        private int vertexIndex, position;

        public PathEntry(WTState state) {
            this.state = state;
            this.vertexIndex = 0;
            this.position = 0;
        }

        public PathEntry(WTState state, int position) {
            this.state = state;
            this.vertexIndex = 0;
            this.position = position;
        }

        public WTState getState() {
            return state;
        }

        public int getVertex() {
            return vertexIndex;
        }

        public void setVertex(int integer) {
            vertexIndex = integer;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public int getPosition() {
            return position;
        }

    }

}
