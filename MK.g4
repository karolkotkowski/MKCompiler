grammar MK;

file                    : (block | statement)+ EOF;

block                   : OPENBLOCK statement+ CLOSEBLOCK;

statement               : (variable_declaration | variable_assignment | command | array_declaration | array_assignment) ENDSTATEMENT;

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
                          | array_element1                  #array_element
                          ;
bracket_expression      : OPENBRACKET expression CLOSEBRACKET;

array_assignment        : array_element1 ASSIGN expression;
array_declaration       : INTSTATEMENT array_declaration1       #int_array_declaration
                          | REALSTATEMENT array_declaration1    #real_array_declaration
                          ;
array_declaration1      : DECLAREVARIABLE NAME OPENARRAY (array_length)? CLOSEARRAY (ASSIGN array_elements)?;
array_elements          : OPENBLOCK expression (NEXTELEMENT expression)* CLOSEBLOCK;
array_length            : INT;
array_index             : expression;
array_element1          : NAME OPENARRAY array_index CLOSEARRAY;

print                   : PRINT expression;

scan                    : SCAN INTSTATEMENT NAME            #scan_int
                          | SCAN REALSTATEMENT NAME         #scan_real
                          | SCAN STRSTATEMENT NAME          #scan_str
                          ;

ENDSTATEMENT            : ';';
NEXTELEMENT             : ',';

OPENBLOCK               : '{';
CLOSEBLOCK              : '}';
OPENBRACKET             : '(';
CLOSEBRACKET            : ')';
OPENARRAY               : '[';
CLOSEARRAY              : ']';

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