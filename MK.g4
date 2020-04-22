grammar MK;

file                    : (block | statement)+ EOF;

block                   : OPENBLOCK statement+ CLOSEBLOCK;

statement               : (variable_declaration | variable_assignment | command) ENDSTATEMENT;

command                 : print;

variable_declaration    : DECLAREVARIABLE NAME (ASSIGN expression)?;
variable_assignment     : NAME ASSIGN expression;

expression      : expression1                       #single1
                          | expression1 (ADD expression)+  #add
                          | expression1 (SUB expression)+  #subtract
                          ;
expression1             : expression2                       #single2
                          | expression2 (MUL expression1)+  #multiply
                          | expression2 (DIV expression1)+  #divide
                          ;
expression2             : bracket_expression                #bracket
                          | NAME                            #name
                          | INT                             #int
                          | REAL                            #real
                          | CHAR                            #char
                          | STR                             #str
                          ;
bracket_expression      : OPENBRACKET expression CLOSEBRACKET;

print                   : PRINT expression;




ENDSTATEMENT            : ';';

OPENBLOCK               : '{';
CLOSEBLOCK              : '}';
OPENBRACKET             : '(';
CLOSEBRACKET            : ')';

ASSIGN                  : '=';

DECLAREVARIABLE         : 'lady';

PRINT                   : 'whisper';
SCAN                    : 'hear';

NAME                    : [a-zA-Z] | [a-zA-Z][a-zA-Z0-9]+;

INT                     : [-+]?DIGIT+;
REAL                    : [-+]?DIGIT'.'DIGIT+;
DIGIT                   : [0-9];

CHAR                    : CHARMARK ~'\''? CHARMARK {setText(getText().substring(1, getText().length()-1));};
CHARMARK                : '\'';
STR                     : STRMARK ~'"'* STRMARK {setText(getText().substring(1, getText().length()-1));};
STRMARK                 : '"';

ADD                     : '+';
SUB                     : '-';
MUL                     : '*';
DIV                     : '/';

WHITESPACE              : [ \t]+ -> skip;
NEWLINE                 : [\n\r]+ -> skip;