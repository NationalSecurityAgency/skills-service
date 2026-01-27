/*
 * Copyright 2025 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
    newDescWelcomeMsg,
    chessGenValue,
    existingDescWelcomeMsg,
    completedMsg,
    gotStartedMsg
}
    from './openai_helper_commands'

describe('Generate Desc Tests', () => {

    it('create project with many skills that have descriptions', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        // Skill 1: JavaScript Arrays
        cy.createSkill(1, 1, 1, {
            name: 'JavaScript Arrays',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript Arrays: Complete Guide

## Creating Arrays
\`\`\`javascript
let fruits = ["apple", "banana", "orange"];
let numbers = new Array(1, 2, 3, 4, 5);
let empty = [];
\`\`\`

## Array Methods
### Adding/Removing Elements
\`\`\`javascript
let arr = [1, 2, 3];
arr.push(4);           // [1, 2, 3, 4]
arr.unshift(0);        // [0, 1, 2, 3, 4]
let last = arr.pop();  // [0, 1, 2, 3]
let first = arr.shift(); // [1, 2, 3]
\`\`\`

### Iterating Arrays
\`\`\`javascript
let colors = ["red", "green", "blue"];
colors.forEach((color, index) => {
    console.log(\`\${index}: \${color}\`);
});
let upperColors = colors.map(color => color.toUpperCase());
\`\`\``
        });

        // Skill 2: JavaScript Functions
        cy.createSkill(1, 1, 2, {
            name: 'JavaScript Functions',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript Functions: Complete Guide

## Function Declarations
### Function Declaration
\`\`\`javascript
function greet(name) {
    return \`Hello, \${name}!\`;
}
\`\`\`

### Function Expression
\`\`\`javascript
const greet = function(name) {
    return \`Hello, \${name}!\`;
};
\`\`\`

### Arrow Functions
\`\`\`javascript
const greet = (name) => \`Hello, \${name}!\`;
const add = (a, b) => a + b;
\`\`\`

## Parameters and Arguments
### Default Parameters
\`\`\`javascript
function greet(name = "Guest", message = "Welcome") {
    return \`\${message}, \${name}!\`;
}
\`\`\`

### Rest Parameters
\`\`\`javascript
function sum(...numbers) {
    return numbers.reduce((total, num) => total + num, 0);
}
\`\`\``
        });

        // Skill 3: JavaScript Objects
        cy.createSkill(1, 1, 3, {
            name: 'JavaScript Objects',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript Objects: Complete Guide

## Creating Objects
\`\`\`javascript
// Object literal
let person = {
    name: "John Doe",
    age: 30,
    city: "New York"
};
\`\`\`

## Object Properties
### Accessing Properties
\`\`\`javascript
let person = { name: "John", age: 30 };

// Dot notation
console.log(person.name);

// Bracket notation
console.log(person["name"]);

// Dynamic property access
let prop = "age";
console.log(person[prop]);
\`\`\`

## Object Methods
\`\`\`javascript
let calculator = {
    a: 0,
    b: 0,
    
    add() {
        return this.a + this.b;
    },
    
    multiply: function() {
        return this.a * this.b;
    }
};
\`\`\`

## Object Destructuring
\`\`\`javascript
let person = { name: "John", age: 30, city: "NYC" };
let { name, age } = person;
// name = "John", age = 30
\`\`\``
        });

        // Skill 4: JavaScript Loops
        cy.createSkill(1, 1, 4, {
            name: 'JavaScript Loops',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript Loops: Complete Guide

## For Loop
\`\`\`javascript
for (let i = 0; i < 5; i++) {
    console.log(\`Iteration \${i}\`);
}
\`\`\`

## While Loop
\`\`\`javascript
let count = 0;
while (count < 5) {
    console.log(\`Count: \${count}\`);
    count++;
}
\`\`\`

## Do-While Loop
\`\`\`javascript
let count = 0;
do {
    console.log(\`Count: \${count}\`);
    count++;
} while (count < 5);
\`\`\`

## For...In Loop (Objects)
\`\`\`javascript
let person = { name: "John", age: 30, city: "NYC" };
for (let key in person) {
    console.log(\`\${key}: \${person[key]}\`);
}
\`\`\`

## For...Of Loop (Arrays/Iterables)
\`\`\`javascript
let colors = ["red", "green", "blue"];
for (let color of colors) {
    console.log(color);
}

// With arrays and index
for (const [index, color] of colors.entries()) {
    console.log(\`\${index}: \${color}\`);
}
\`\`\`

## Loop Control
\`\`\`javascript
for (let i = 0; i < 10; i++) {
    if (i === 3) continue;  // Skip iteration 3
    if (i === 7) break;     // Exit loop at 7
    console.log(i);
}
\`\`\``
        });

        // Skill 5: JavaScript Conditionals
        cy.createSkill(1, 1, 5, {
            name: 'JavaScript Conditionals',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript Conditionals: Complete Guide

## If Statement
\`\`\`javascript
let age = 18;
if (age >= 18) {
    console.log("You are an adult");
}
\`\`\`

## If-Else Statement
\`\`\`javascript
let age = 16;
if (age >= 18) {
    console.log("You are an adult");
} else {
    console.log("You are a minor");
}
\`\`\`

## If-Else If-Else
\`\`\`javascript
let grade = 85;
if (grade >= 90) {
    console.log("A");
} else if (grade >= 80) {
    console.log("B");
} else if (grade >= 70) {
    console.log("C");
} else {
    console.log("F");
}
\`\`\`

## Switch Statement
\`\`\`javascript
let day = "Monday";
switch (day) {
    case "Monday":
        console.log("Start of the week");
        break;
    case "Friday":
        console.log("Almost weekend");
        break;
    default:
        console.log("Regular day");
}
\`\`\`

## Ternary Operator
\`\`\`javascript
let age = 18;
let message = age >= 18 ? "Adult" : "Minor";

// Nested ternary
let grade = 85;
let result = grade >= 90 ? "A" : 
             grade >= 80 ? "B" : 
             grade >= 70 ? "C" : "F";
\`\`\``
        });

        // Skill 6: JavaScript DOM Manipulation
        cy.createSkill(1, 1, 6, {
            name: 'JavaScript DOM Manipulation',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript DOM Manipulation: Complete Guide

## Selecting Elements
\`\`\`javascript
// By ID
let element = document.getElementById("myElement");

// By class name
let elements = document.getElementsByClassName("myClass");

// CSS selectors
let element = document.querySelector("#myElement");
let elements = document.querySelectorAll(".myClass");
\`\`\`

## Modifying Content
\`\`\`javascript
let element = document.getElementById("myElement");

// Text content
element.textContent = "New text content";

// HTML content
element.innerHTML = "<strong>Bold text</strong>";

// Value (for form elements)
let input = document.getElementById("myInput");
input.value = "New value";
\`\`\`

## Modifying Attributes
\`\`\`javascript
let img = document.getElementById("myImage");
img.setAttribute("alt", "Description");
img.src = "new-image.jpg";
img.id = "newId";
\`\`\`

## Creating Elements
\`\`\`javascript
let newDiv = document.createElement("div");
newDiv.textContent = "New element";
newDiv.className = "my-class";
document.body.appendChild(newDiv);
\`\`\``
        });

        // Skill 7: JavaScript Events
        cy.createSkill(1, 1, 7, {
            name: 'JavaScript Events',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript Events: Complete Guide

## Event Listeners
\`\`\`javascript
let button = document.getElementById("myButton");

button.addEventListener("click", function(event) {
    console.log("Button clicked!");
    console.log("Event object:", event);
});

// Arrow function
button.addEventListener("click", (event) => {
    console.log("Target:", event.target);
});
\`\`\`

## Common Event Types
\`\`\`javascript
// Mouse events
element.addEventListener("click", handler);
element.addEventListener("dblclick", handler);
element.addEventListener("mouseover", handler);
element.addEventListener("mouseout", handler);

// Keyboard events
element.addEventListener("keydown", handler);
element.addEventListener("keyup", handler);
element.addEventListener("keypress", handler);

// Form events
form.addEventListener("submit", handler);
input.addEventListener("change", handler);
input.addEventListener("input", handler);
\`\`\`

## Event Object
\`\`\`javascript
button.addEventListener("click", function(event) {
    console.log("Type:", event.type);           // "click"
    console.log("Target:", event.target);       // The element clicked
    console.log("Current target:", event.currentTarget);
    console.log("Timestamp:", event.timeStamp);
    
    // Mouse position
    console.log("X:", event.clientX);
    console.log("Y:", event.clientY);
});
\`\`\`

## Event Delegation
\`\`\`javascript
// Instead of adding listeners to many elements
document.getElementById("parent").addEventListener("click", function(event) {
    if (event.target.classList.contains("child")) {
        console.log("Child element clicked:", event.target.textContent);
    }
});
\`\`\``
        });

        // Skill 8: JavaScript Async Programming - Promises
        cy.createSkill(1, 1, 8, {
            name: 'JavaScript Async Programming - Promises',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript Promises: Complete Guide

## Creating Promises
\`\`\`javascript
// Basic promise
let promise = new Promise(function(resolve, reject) {
    setTimeout(function() {
        let success = true;
        if (success) {
            resolve("Operation successful!");
        } else {
            reject("Operation failed!");
        }
    }, 1000);
});
\`\`\`

## Consuming Promises
\`\`\`javascript
promise
    .then(function(result) {
        console.log("Success:", result);
    })
    .catch(function(error) {
        console.error("Error:", error);
    })
    .finally(function() {
        console.log("Operation completed");
    });
\`\`\`

## Chaining Promises
\`\`\`javascript
fetchUser(1)
    .then(function(user) {
        console.log("User:", user);
        return fetchUserPosts(user.id);  // Return new promise
    })
    .then(function(posts) {
        console.log("Posts:", posts);
        return fetchPostComments(posts[0].id);
    })
    .then(function(comments) {
        console.log("Comments:", comments);
    })
    .catch(function(error) {
        console.error("Error in chain:", error);
    });
\`\`\`

## Promise Methods
\`\`\`javascript
// Promise.all - All promises must succeed
Promise.all([
    fetchUser(1),
    fetchPosts(1),
    fetchComments(1)
])
.then(function(results) {
    let [user, posts, comments] = results;
    console.log("All data loaded:", { user, posts, comments });
})
.catch(function(error) {
    console.error("One promise failed:", error);
});

// Promise.race - First promise to resolve/reject
Promise.race([
    fetchFromCache(),
    fetchFromServer()
])
.then(function(result) {
    console.log("First result:", result);
});
\`\`\``
        });

        // Skill 9: JavaScript Async/Await
        cy.createSkill(1, 1, 9, {
            name: 'JavaScript Async/Await',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript Async/Await: Complete Guide

## Basic Async/Await
\`\`\`javascript
// Promise-based
function fetchData() {
    return new Promise(resolve => {
        setTimeout(() => resolve("Data loaded"), 1000);
    });
}

// Using async/await
async function getData() {
    try {
        let result = await fetchData();
        console.log(result); // "Data loaded"
    } catch (error) {
        console.error("Error:", error);
    }
}

getData();
\`\`\`

## Async Functions
\`\`\`javascript
// Async function always returns a promise
async function example() {
    return "Hello";  // Automatically wrapped in Promise.resolve()
}

example().then(console.log); // "Hello"

// Async function with error
async function errorExample() {
    throw new Error("Something went wrong");
}

errorExample().catch(console.error);
\`\`\`

## Error Handling
\`\`\`javascript
async function fetchUserData(userId) {
    try {
        let user = await fetchUser(userId);
        let posts = await fetchUserPosts(user.id);
        let comments = await fetchPostComments(posts[0].id);
        
        return { user, posts, comments };
    } catch (error) {
        console.error("Failed to fetch data:", error);
        throw error; // Re-throw if needed
    }
}
\`\`\`

## Multiple Awaits
\`\`\`javascript
// Sequential (slower)
async function sequential() {
    let user = await fetchUser(1);
    let posts = await fetchPosts(1);
    let comments = await fetchComments(1);
    return { user, posts, comments };
}

// Parallel (faster)
async function parallel() {
    let [user, posts, comments] = await Promise.all([
        fetchUser(1),
        fetchPosts(1),
        fetchComments(1)
    ]);
    return { user, posts, comments };
}
\`\`\`

## Async/Await with Loops
\`\`\`javascript
// Sequential processing
async function processSequentially(items) {
    for (let item of items) {
        let result = await processItem(item);
        console.log("Processed:", result);
    }
}

// Parallel processing
async function processInParallel(items) {
    let promises = items.map(item => processItem(item));
    let results = await Promise.all(promises);
    return results;
}
\`\`\``
        });

        // Skill 10: JavaScript ES6+ Features
        cy.createSkill(1, 1, 10, {
            name: 'JavaScript ES6+ Features',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# ES6+ Features: Modern JavaScript Guide

## Arrow Functions
\`\`\`javascript
// Traditional function
function add(a, b) {
    return a + b;
}

// Arrow function
const add = (a, b) => a + b;

// Single parameter (no parentheses needed)
const square = x => x * x;

// Multiple statements (curly braces required)
const greet = (name) => {
    const message = \`Hello, \${name}!\`;
    return message;
};
\`\`\`

## Template Literals
\`\`\`javascript
const name = 'John';
const age = 30;

// String interpolation
const message = \`Hello, \${name}! You are \${age} years old.\`;

// Multi-line strings
const html = \`
    <div>
        <h1>\${name}</h1>
        <p>Age: \${age}</p>
    </div>
\`;
\`\`\`

## Destructuring
\`\`\`javascript
// Object destructuring
const person = { name: 'John', age: 30, city: 'NYC' };
const { name, age } = person;
// name = 'John', age = 30

// Array destructuring
const colors = ['red', 'green', 'blue'];
const [first, second] = colors;
// first = 'red', second = 'green'

// Function parameters
function createUser({ name, age, city = 'Unknown' }) {
    return { name, age, city };
}
\`\`\`

## Spread and Rest Operators
\`\`\`javascript
// Spread with arrays
const arr1 = [1, 2, 3];
const arr2 = [4, 5, 6];
const combined = [...arr1, ...arr2]; // [1, 2, 3, 4, 5, 6]

// Spread with objects
const person = { name: 'John', age: 30 };
const employee = { ...person, job: 'Developer' };
// { name: 'John', age: 30, job: 'Developer' }

// Rest parameters
function sum(...numbers) {
    return numbers.reduce((total, num) => total + num, 0);
}
\`\`\`

## Classes
\`\`\`javascript
class Person {
    constructor(name, age) {
        this.name = name;
        this.age = age;
    }
    
    greet() {
        return \`Hello, I'm \${this.name}\`;
    }
    
    static createAdult(name) {
        return new Person(name, 18);
    }
}

// Inheritance
class Employee extends Person {
    constructor(name, age, job) {
        super(name, age); // Call parent constructor
        this.job = job;
    }
}
\`\`\`
`
        });

        // Skill 11: JavaScript Variables
        cy.createSkill(1, 1, 11, {
            name: 'JavaScript Variables',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript Variables

## Variable Declarations
\`\`\`javascript
// var (function-scoped)
var message = "Hello";

// let (block-scoped)
let count = 42;

// const (block-scoped, immutable)
const PI = 3.14159;
\`\`\`

## Best Practices
- Use \`const\` by default
- Use \`let\` only when reassigning
- Avoid \`var\` in modern code
- Use meaningful names`
        });

        // Skill 12: JavaScript Data Types
        cy.createSkill(1, 1, 12, {
            name: 'JavaScript Data Types',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript Data Types

## Primitive Types
\`\`\`javascript
let name = "John";        // String
let age = 30;             // Number
let isActive = true;       // Boolean
let empty = null;          // Null
let undefinedVar;          // Undefined
\`\`\`

## Reference Types
\`\`\`javascript
let person = { name: "John" };  // Object
let colors = ["red", "blue"];   // Array
let greet = function() {};      // Function
\`\`\`

## Type Checking
\`\`\`javascript
typeof "hello"  // "string"
typeof 42       // "number"
typeof {}       // "object"
\`\`\``
        });

        // Skill 13: JavaScript String Methods
        cy.createSkill(1, 1, 13, {
            name: 'JavaScript String Methods',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript String Methods

## Common Methods
\`\`\`javascript
let text = "Hello World";

// Length and access
text.length;           // 11
text[0];               // "H"
text.charAt(0);        // "H"

// Case conversion
text.toUpperCase();    // "HELLO WORLD"
text.toLowerCase();    // "hello world"

// Searching
text.includes("World");    // true
text.indexOf("World");     // 6
text.startsWith("Hello");  // true
\`\`\`

## String Manipulation
\`\`\`javascript
// Slicing
text.slice(0, 5);      // "Hello"
text.substring(0, 5);  // "Hello"

// Trimming
let padded = "  hello  ";
padded.trim();         // "hello"

// Splitting
"apple,banana,orange".split(",");  // ["apple", "banana", "orange"]
\`\`\``
        });

        // Skill 14: JavaScript Number Methods
        cy.createSkill(1, 1, 14, {
            name: 'JavaScript Number Methods',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript Number Methods

## Number Methods
\`\`\`javascript
let num = 123.456;

// Rounding
Math.round(num);    // 123
Math.floor(num);    // 123
Math.ceil(num);     // 124
Math.trunc(num);    // 123

// Decimal places
num.toFixed(2);     // "123.46"
num.toPrecision(4); // "123.5"

// Other operations
Math.abs(-5);       // 5
Math.max(1, 2, 3);  // 3
Math.min(1, 2, 3);  // 1
Math.random();      // 0-1 random number
\`\`\`

## Number Conversion
\`\`\`javascript
// String to number
parseInt("42");         // 42
parseFloat("3.14");     // 3.14
Number("42");           // 42

// Number to string
let num = 42;
num.toString();         // "42"
String(num);            // "42"
\`\`\``
        });

        // Skill 15: JavaScript Math Object
        cy.createSkill(1, 1, 15, {
            name: 'JavaScript Math Object',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript Math Object

## Basic Math Functions
\`\`\`javascript
// Powers and roots
Math.pow(2, 3);        // 8
Math.sqrt(16);         // 4
Math.cbrt(8);          // 2

// Trigonometry
Math.sin(Math.PI / 2); // 1
Math.cos(0);           // 1
Math.tan(Math.PI / 4); // 1

// Logarithms
Math.log(Math.E);       // 1
Math.log10(100);       // 2
Math.log2(8);          // 3
\`\`\`

## Constants
\`\`\`javascript
Math.PI;               // 3.14159...
Math.E;                // 2.71828...
Math.SQRT2;            // 1.41421...
Math.SQRT1_2;          // 0.70710...
\`\`\`

## Random Numbers
\`\`\`javascript
// Random between 0 and 1
Math.random();

// Random integer between min and max
function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}
\`\`\``
        });

        // Skill 16: JavaScript Date Object
        cy.createSkill(1, 1, 16, {
            name: 'JavaScript Date Object',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript Date Object

## Creating Dates
\`\`\`javascript
// Current date
let now = new Date();

// Specific date
let birthday = new Date(1990, 0, 15); // January 15, 1990
let specific = new Date("2023-12-25T10:30:00");

// From timestamp
let fromTimestamp = new Date(1672531200000);
\`\`\`

## Getting Date Parts
\`\`\`javascript
let date = new Date();
date.getFullYear();     // 2023
date.getMonth();        // 0-11 (0 = January)
date.getDate();         // 1-31
date.getDay();          // 0-6 (0 = Sunday)
date.getHours();        // 0-23
date.getMinutes();      // 0-59
date.getSeconds();      // 0-59
\`\`\`

## Date Methods
\`\`\`javascript
// Formatting
date.toDateString();    // "Mon Dec 25 2023"
date.toTimeString();    // "10:30:00 GMT+0000"
date.toISOString();     // "2023-12-25T10:30:00.000Z"

// Manipulation
date.setDate(date.getDate() + 7);  // Add 7 days
date.setMonth(date.getMonth() + 1); // Add 1 month
\`\`\``
        });

        // Skill 17: JavaScript Regular Expressions
        cy.createSkill(1, 1, 17, {
            name: 'JavaScript Regular Expressions',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript Regular Expressions

## Creating Regex
\`\`\`javascript
// Literal notation
let pattern = /hello/;

// Constructor function
let pattern2 = new RegExp("hello");

// With flags
let pattern3 = /hello/gi; // global, case-insensitive
\`\`\`

## Common Patterns
\`\`\`javascript
// Email validation
let emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Phone number
let phonePattern = /^\d{3}-\d{3}-\d{4}$/;

// Password (8+ chars, uppercase, lowercase, number)
let passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
\`\`\`

## Regex Methods
\`\`\`javascript
let text = "Hello World";
let pattern = /hello/i;

// Test for match
pattern.test(text);     // true

// Find match
text.match(pattern);    // ["Hello"]

// Replace
text.replace(/world/i, "JavaScript"); // "Hello JavaScript"
\`\`\``
        });

        // Skill 18: JavaScript Error Handling
        cy.createSkill(1, 1, 18, {
            name: 'JavaScript Error Handling',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript Error Handling

## Try-Catch Blocks
\`\`\`javascript
try {
    let result = riskyOperation();
    console.log(result);
} catch (error) {
    console.error('Error:', error.message);
} finally {
    console.log('Always runs');
}
\`\`\`

## Error Types
\`\`\`javascript
// ReferenceError
try {
    console.log(undefinedVar);
} catch (error) {
    console.log(error.name); // "ReferenceError"
}

// TypeError
try {
    null.someMethod();
} catch (error) {
    console.log(error.name); // "TypeError"
}
\`\`\`

## Throwing Errors
\`\`\`javascript
function validateAge(age) {
    if (typeof age !== 'number') {
        throw new TypeError('Age must be a number');
    }
    if (age < 0) {
        throw new RangeError('Age cannot be negative');
    }
    return true;
}
\`\`\``
        });

        // Skill 19: JavaScript JSON
        cy.createSkill(1, 1, 19, {
            name: 'JavaScript JSON',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript JSON

## Parsing JSON
\`\`\`javascript
let jsonString = '{"name": "John", "age": 30, "city": "NYC"}';

// Parse JSON string to object
let person = JSON.parse(jsonString);
console.log(person.name); // "John"

// Handle parsing errors
try {
    let data = JSON.parse(invalidJson);
} catch (error) {
    console.error('Invalid JSON:', error);
}
\`\`\`

## Stringifying JSON
\`\`\`javascript
let person = {
    name: "John",
    age: 30,
    hobbies: ["coding", "reading"]
};

// Convert object to JSON string
let jsonString = JSON.stringify(person);
// '{"name":"John","age":30,"hobbies":["coding","reading"]}'

// Pretty print
let prettyJson = JSON.stringify(person, null, 2);
\`\`\`

## JSON Methods
\`\`\`javascript
// With replacer function
let filtered = JSON.stringify(person, ["name", "age"]);

// With replacer function
let custom = JSON.stringify(person, (key, value) => {
    if (key === "age") return undefined; // Exclude age
    return value;
});
\`\`\``
        });

        // Skill 20: JavaScript Local Storage
        cy.createSkill(1, 1, 20, {
            name: 'JavaScript Local Storage',
            selfReportingType: 'HonorSystem',
            numPerformToCompletion: 1,
            description: `# JavaScript Local Storage

## localStorage Methods
\`\`\`javascript
// Store data
localStorage.setItem('username', 'John');
localStorage.setItem('theme', 'dark');

// Get data
let username = localStorage.getItem('username'); // "John"
let theme = localStorage.getItem('theme');       // "dark"

// Remove item
localStorage.removeItem('theme');

// Clear all
localStorage.clear();
\`\`\`

## Storing Objects
\`\`\`javascript
let user = {
    name: "John",
    age: 30,
    preferences: {
        theme: "dark",
        language: "en"
    }
};

// Store as JSON string
localStorage.setItem('user', JSON.stringify(user));

// Retrieve and parse
let storedUser = JSON.parse(localStorage.getItem('user'));
\`\`\`

## sessionStorage
\`\`\`javascript
// Same methods as localStorage
sessionStorage.setItem('tempData', 'value');
let temp = sessionStorage.getItem('tempData');

// Data cleared when tab closes
\`\`\`

## Storage Events
\`\`\`javascript
// Listen for storage changes
window.addEventListener('storage', (event) => {
    console.log('Storage changed:', event.key, event.newValue);
});
\`\`\``
        });

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')

    });

});


