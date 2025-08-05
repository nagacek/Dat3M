grammar Cat;

@header{
import com.dat3m.dartagnan.wmm.axiom.*;
}

mcm
    :   (NAME)* (QUOTED_STRING)? (definition | include | show | pattern)+ EOF
    ;

definition
    :   axiomDefinition
    |   letFuncDefinition
    |   letDefinition
    |   letRecDefinition
    ;

axiomDefinition locals [Class<?> cls]
    :   (flag = FLAG | undef = UNDEFINED)? (negate = NOT)? ACYCLIC { $cls = Acyclicity.class; } e = expression (AS NAME)?
    |   (flag = FLAG | undef = UNDEFINED)? (negate = NOT)? IRREFLEXIVE { $cls = Irreflexivity.class; } e = expression (AS NAME)?
    |   (flag = FLAG | undef = UNDEFINED)? (negate = NOT)? EMPTY { $cls = Emptiness.class; } e = expression (AS NAME)?
    ;

letFuncDefinition
    :   LET (fname = NAME) LPAR params = parameterList RPAR EQ e = expression
    ;

letDefinition
    :   LET n = NAME EQ e = expression
    ;

letRecDefinition
    :   LET REC n = NAME EQ e = expression letRecAndDefinition*
    ;

letRecAndDefinition
    :   AND n = NAME EQ e = expression
    ;

expression
    :   e1 = expression STAR e2 = expression                            # exprCartesian
    |   e = expression (POW)? STAR                                      # exprTransRef
    |   e = expression (POW)? PLUS                                      # exprTransitive
    |   e = expression (POW)? INV                                       # exprInverse
    |   e = expression OPT                                              # exprOptional
    |   NOT e = expression                                              # exprComplement
    |   e1 = expression AMP e2 = expression                             # exprIntersection
    |   e1 = expression BSLASH e2 = expression                          # exprMinus
    |   e1 = expression SEMI e2 = expression                            # exprComposition
    |   e1 = expression BAR e2 = expression                             # exprUnion
    |   LBRAC DOMAIN LPAR e = expression RPAR RBRAC                     # exprDomainIdentity
    |   LBRAC RANGE LPAR e = expression RPAR RBRAC                      # exprRangeIdentity
    |   (TOID LPAR e = expression RPAR | LBRAC e = expression RBRAC)    # exprIdentity
    |   LPAR e = expression RPAR                                        # expr
    |   n = NAME                                                        # exprBasic
    |   call = NEW LPAR RPAR                                            # exprNew
    |   call = NAME LPAR args = argumentList RPAR                       # exprCall
    ;

pattern
    : PATTERN edge (COMMA edge)*
    ;

edge
    :   n1 = node LARR NEG e = expression RARR n2 = node        # negEdge
    |   n1 = node LARR e = expression RARR n2 = node            # posEdge
    ;

node
    :   id = NUMBER (LPAR set (COMMA set)* RPAR)?
    ;

set
    : e = expression        # exprSet
    | EXEC                  # execSet
    ;

include
    :   INCLUDE path = QUOTED_STRING
    ;

show
    :   SHOW expression (AS NAME)? (COMMA expression (AS NAME)?)*
    ;

parameterList
    : (NAME (COMMA NAME)*)
    ;

argumentList
    : expression (COMMA expression)*
    ;

LET     :   'let';
REC     :   'rec';
AND     :   'and';
AS      :   'as';
TOID    :   'toid';
SHOW    :   'show';
INCLUDE :   'include';
PATTERN :   'pattern';

ACYCLIC     :   'acyclic';
IRREFLEXIVE :   'irreflexive';
EMPTY       :   'empty';

EQ      :   '=';
STAR    :   '*';
PLUS    :   '+';
OPT     :   '?';
INV     :   '-1';
NOT     :   '~';
AMP     :   '&';
BAR     :   '|';
SEMI    :   ';';
BSLASH  :   '\\';
POW     :   ('^');
NEG     :   '¬';

LPAR    :   '(';
RPAR    :   ')';
LBRAC   :   '[';
RBRAC   :   ']';
COMMA   :   ',';
LARR    :   '-';
RARR    :   '->';

DOMAIN      :   'domain';
RANGE       :   'range';
NEW         :   'new';
EXEC        :   'exec';

FLAG       :   'flag';
UNDEFINED  :   'undefined_unless';

QUOTED_STRING : '"' .*? '"';

NUMBER      :   [0-9]+;
NAME        :   [A-Za-z0-9\-_.]+ | NUMBER;

LINE_COMMENT
    :   '//' ~[\n]*
        -> skip
    ;

BLOCK_COMMENT
    :   ('(*' (.)*? '*)' | '/*' (.)*? '*/')
        -> skip
    ;

WS
    :   [ \t\r\n]+
        -> skip
    ;
