grammar MK;

file                    : (block | statement)+ EOF;

block                   : OPENBLOCK statement+ CLOSEBLOCK;

statement               : (variable_declaration | variable_assignment | command) ENDSTATEMENT;

command                 : print | scan;

variable_declaration    : DECLAREVARIABLE NAME (ASSIGN expression)?;
variable_assignment     : NAME ASSIGN expression;

expression              : expression1                       #single1
                          | expression ADD expression1      #add
                          | expression SUB expression1      #subtract
                          ;
expression1             : expression2                       #single2
                          | expression1 MUL expression2     #multiply
                          | expression1 DIV expression2     #divide
                          ;
expression2             : bracket_expression                #bracket
                          | NAME                            #name
                          | INT                             #int
                          | REAL                            #real
                          | CHAR                            #char
                          | STR                             #str
                          ;
bracket_expression      : OPENBRACKET expression CLOSEBRACKET;

print                   : PRINT (NAME | expression);

scan                    : SCAN INTSTATEMENT NAME            #scan_int
                          | SCAN REALSTATEMENT NAME         #scan_real
                          | SCAN STRSTATEMENT NAME          #scan_str
                          ;

ENDSTATEMENT            : ';';

OPENBLOCK               : '{';
CLOSEBLOCK              : '}';
OPENBRACKET             : '(';
CLOSEBRACKET            : ')';

ASSIGN                  : '=';

DECLAREVARIABLE         : 'lady';

PRINT                   : 'whisper';
SCAN                    : 'hear';

INTSTATEMENT            : 'int';
REALSTATEMENT           : 'real';
STRSTATEMENT            : 'str';


NAME                    : [a-zA-Z] | [a-zA-Z][a-zA-Z0-9]+;

INT                     : [-+]?DIGIT+;
REAL                    : [-+]?DIGIT+ '.' DIGIT+;
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