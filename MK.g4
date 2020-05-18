grammar MK;

file                    : (statement  | function_declaration | instruction)* EOF;

block                   : OPENBLOCK (statement | instruction)* CLOSEBLOCK;

statement               : (variable_declaration | variable_assignment | command | array_declaration | array_assignment | function_call) ENDSTATEMENT;

command                 : print | scan | returning;

instruction             : IFSTATEMENT instruction1 block          #if_instruction
                          | WHILESTATEMENT instruction1 block     #while_instruction
                          ;
instruction1            : OPENBRACKET expression COMPARESTATEMENT expression CLOSEBRACKET;

variable_declaration    : INTSTATEMENT variable_declaration1    #int_variable_declaration
                          | REALSTATEMENT variable_declaration1 #real_variable_declaration
                          ;
variable_declaration1   : DECLAREVARIABLE NAME;
variable_assignment     : NAME ASSIGN expression;

function_declaration    : function_declaration1 block;
function_declaration1   : INTSTATEMENT function_declaration2    #int_function_declaration
                          | REALSTATEMENT function_declaration2 #real_function_declaration
                          ;
function_declaration2   : DECLAREVARIABLE NAME arguments;
function_call           : NAME call_arguments;
returning               : RETURN expression;

arguments               : OPENBRACKET (argument (NEXTELEMENT argument)* )* CLOSEBRACKET;
call_arguments          : OPENBRACKET (expression (NEXTELEMENT expression)* )* CLOSEBRACKET;
argument                : INTSTATEMENT NAME         #int_argument
                          | REALSTATEMENT NAME      #real_argument
                          ;

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
                          | NAME call_arguments             #function
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

scan                    : SCAN NAME;

ENDSTATEMENT            : ';';
NEXTELEMENT             : ',';

OPENBLOCK               : '{';
CLOSEBLOCK              : '}';
OPENBRACKET             : '(';
CLOSEBRACKET            : ')';
OPENARRAY               : '[';
CLOSEARRAY              : ']';

ASSIGN                  : '=';

RETURN                  : 'give ';

DECLAREVARIABLE         : 'lady ';

PRINT                   : 'whisper ';
SCAN                    : 'hear ';

INTSTATEMENT            : 'int ';
REALSTATEMENT           : 'real ';
STRSTATEMENT            : 'str ';

IFSTATEMENT             : 'if';
WHILESTATEMENT          : 'while';

COMPARESTATEMENT        : '==' | '<' | '<=' | '>=' | '>' | '!=';

NAME                    : [a-zA-Z][a-zA-Z0-9]*;

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