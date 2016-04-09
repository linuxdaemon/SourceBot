# SourceBot

This is a bot for the Tumblr blogging platform. It is designed to search 
through a configured list of tags for posts, and provide them to the 
user, either through the drafts or queue.

[Set up required before installing the bot](#setup)

[Installing the bot](#installing)

## Setup

#### Requirements
* [Java 7+](#setup-java)
* [PostgreSQL Database](#setup-database)
* [Tumblr API Authentication](#tumblr-authentication)

### Setup Database
The Bot requires a MySQL database for configuration and caching purposes

#### Installation
This project uses version 9.4 of the PostgreSQL server

[PostgreSQL Download](http://www.postgresql.org/download/)

#### Creating the Database
Ok, so now that you have PostgreSQL installed, we need to create a database 
for the bot to use

For most installation, I would recommend using [PgAdmin](http://www.pgadmin.org/index.php) for configuring the database


And now you have the initial database set up

#### Setting up a User
The bot will need an account with which to access the database, and, for
security reasons, we don't want to use the root user

1. To create the user, run `CREATE USER '<username>'@'<ip-address>' IDENTIFIED BY '<password>';` at the MySQL console. ex: `CREATE USER 'sourcebot'@'localhost' IDENTIFIED BY 'password123';` In most cases, the IP Address will be `localhost`, but if you plan on running the database on a machine separate from the bot itself, you will need to specify the bot machine's IP Address
2. Now we need to give the user access to the database, to do that, run `GRANT ALL PRIVILEGES ON <database name> TO '<username>'@'<ip-address>';` ex: `GRANT ALL PRIVILEGES ON sourcebot.* TO 'sourcebot'@'localhost';`
3. Ensure the privileges have been updated by running `FLUSH PRIVILEGES;`

Now you either create the tables manually, or they will be created for 
you when you [install the program](#installing)

#### Tables
* `blogs` Used for storing configuration for each registered blog. Columns:
    * `id int NOT NULL AUTO_INCREMENT PRIMARY KEY`
    * `url text NOT NULL`
    * `blog_check_active boolean NOT NULL DEFAULT TRUE`
    * `sample_size int NOT NULL DEFAULT '1000'`
    * `post_type text`
    * `post_select text`
    * `post_state text NOT NULL`
    * `post_buffer int NOT NULL DEFAULT 20`
    * `post_comment text`
    * `post_tags text`
    * `preserve_tags boolean NOT NULL DEFAULT FALSE`
    * `active boolean NOT NULL DEFAULT FALSE`
    
* `search_rules` Used for storing all the search rules, allowed/blocked blogs/tags. Columns:
    * `id int NOT NULL AUTO_INCREMENT PRIMARY KEY`
    * `url text NOT NULL`
    * `type text NOT NULL`
    * `action text NOT NULL`
    * `term text NOT NULL`
    
* `seen_posts` A table to store all the post the bot has reblogged, on a per blog basis, so it will not repeatedly reblog posts. Columns:
    * `id int NOT NULL AUTO_INCREMENT PRIMARY KEY`
    * `url TEXT NOT NULL`
    * `post_id bigint NOT NULL`
    * `tag test NOT NULL`
    * `blog text NOT NULL`
    * `rb_id bigint NOT NULL`
    
* `tag_stats` A table for storing information on tag searches, for the purposes of statistical measurement. This table may be made optioninal in the future, but it required as of v0.1. Columns:
    * `id int NOT NULL AUTO_INCREMENT PRIMARY KEY`
    * `url text NOT NULL`
    * `tag text NOT NULL`
    * `time datetime NOT NULL DEFAULT NOW()`
    * `search_time int NOT NULL`
    * `search int NOT NULL`
    * `selected int NOT NULL`
    
### Tumblr API
One of the requirements for this bot is a Tumblr API connection, which requires an API key

1. If you do not already have a Tumblr account, you will need to register at [tumblr.com](http://tumblr.com)
2. You will need to register an OAuth Application with Tumblr, to do this, go [here](http://tumblr.com/oauth/register)
    * `Application Name` Can be anything ex: 'SourceBot'
    * `Application Website` Can be any valid website ex: 'http;//github.com'
    * `Application Description` A short description of the bot ex: 'A content curator bot, configured by the user to search through tags for interesting posts'
    * `Administrative Contact Email` An email Tumblr may contact you at if there is a problem with your application's connection to the API ex: 'bob@example.com'
    * `Default Callback URL` Not needed for the bot, set it to '/'
3. Click Register and the application should go through
4. Once you have your application registered, you should see a page listing all your registered application, most likely just this one
5. Click `Explore API` under your Application's name
6. Allow the API console access to your account
7. Click the `Java` tab on the console, the should show something like

<code><pre>
// Authenticate via OAuth
 JumblrClient client = new JumblrClient(
   "&lt;consumer key&gt;",
   "&lt;consumer secret&gt;"
 );
 client.setToken(
   "&lt;token&gt;",
   "&lt;token secret&gt;"
 );
 
 // Make the request
 User user = client.user();
 </pre></code>
 
 You will need to keep this window open during [installation](#installing) or copy the consumer key, consumer secret, token, and token secret to a file
    
## Installing
To install the bot, you first need a [mysql database](#setup-database) and a [Tumblr API key](#tumblr-api)

1. Download the newest SourceBot release [here](https://github.com/linuxdemon1/SourceBot/releases)
2. Unzip the archive in to a directory
3. Now we need to run the install script
    * Windows
        * Open a Command Prompt and change in to the directory where you extracted the archive. ex: `cd \Users\<username>\SourceBot-{version number}`
        * Now enter the `bin` sub directory `cd bin`
        * `SourceBot.bat install`
        
    * Linux
        * Open a Terminal and change in to the directory where you extracted the archive, ex `cd ~/SourceBot-{version number}`
        * Now enter the `bin` sub directory `cd bin`
        * `./SourceBot install` (if the terminal gives a response like `Permission Denied` try running `chmod +x SourceBot` and try again)
        
You should be all set, now you need to add some search rules before running the bot for the first time