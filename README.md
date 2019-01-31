easy-springfield
================

Tools for managing a Springfield WebTV server.


SYNOPSIS
--------

    easy-springfield list-users [<domain>]
    easy-springfield list-collections <user> [<domain>]
    easy-springfield create-user <user> [<domain>]
    easy-springfield create-collection [-t, --title <arg>] [-d, --description <arg>] \
      <collection> <user> [<domain>]
    easy-springfield create-presentation [-t, --title <arg>] [-d, --description <arg>] \
      [-r, --require-ticket] <user> [<domain>]
    easy-springfield create-springfield-actions [-c, --check-parent-items] [-v, --videos-folder <arg>] \
      <videos-csv> > springfield-actions.xml
    easy-springfield status [-u, --user <arg>][-d, --domain <arg>]
    easy-springfield set-require-ticket <springfield-path> {true|false}
    easy-springfield create-ticket [-e,--expires-after-seconds <arg>] [-t, --ticket <arg>] \
      <springfield-path>
    easy-springfield delete-ticket <ticket>
    easy-springfield delete [-r, --with-referenced-items] <springfield-path>
    easy-springfield add-videoref-to-presentation <video> <name> <presentation>
    easy-springfield add-presentationref-to-collection <presentation> <name> <collection>
    easy-springfield add-subtitles-to-video --language <code> <video> <web-vtt-file>
    easy-springfield add-subtitles-to-presentation --language <code> <presentation> <web-vtt-file>...

Note:  `add-subtitles-to-video` and `add-subtitles-to-presentation` are still to be implememented.

DESCRIPTION
-----------
[Springfield Web TV] is a platform for delivering A/V media files over the web. Managing
the hosted content of a Springfield instance can be a rather challenging task, due to the
lack of supporting tools. `easy-springfield` provides some commands to ease this task. It
does *not* presume to fully automate Springfield management.

(Note: even though Springfield can also serve audio-only media files, all media files are generall
referred to as "videos" in the Springfield interface. We will do the same below.)

### Springfield paths
Videos in Springfield are stored in a tree-structure. The service `smithers2` keeps
track of this structure and offers a RESTful API to it. `easy-springfield` uses this RESTful
API. Where the commands require a `springfield-path` argument, a path into aforementioned tree is intended.

Some examples:

    domain/dans/user 
    domain/dans/user/getuigen
    domain/dans/user/getuigen/collection/ww2
    domain/dans/user/getuigen/collection/ww2/presentation/easy-dataset:12345
    domain/dans/user/history/presentation/8
    domain/dans/user/history/video/1
    
As you can see in the last two examples, presentations and videos are stored directly under the user
(in this case called "history"). Presentations *reference* videos by springfield path to include
them, and collections *reference* presentations to do the same.

It is possible to configure a *default domain*. This allows you to leave out the domain of the springfield
paths. If you configure the default domain to `dans` the above examples then become:

    user 
    user/getuigen
    user/getuigen/collection/ww2
    user/getuigen/collection/ww2/presentation/easy-dataset:12345
    user/history/presentation/8
    user/history/video/1

Everywhere else where a domain must be specified, it will then also default to the value you configured.

#### Referids
In some cases springfield paths are used to include an item by reference into another item. For example a
collection is built up using springfield-paths to presentations. These springfield-paths are then called
`referid`s. 

### Examining raw Springfield metadata 
Although `easy-springfield` lets you manage a considerable part of the Springfield repository without your
having to interact with `smithers2` directly, it is often convenient to examine its raw output. You can
construct the smithers-URL of an item as follows:

    <smithers2-base-uri>/<springfield-path>
    
`smithers2-base-uri` can be found in `application.properties`. `springfield-path` must of course include
the domain. To get all the metadata for user `getuigen` in the above example you would therefore open the
following URL in your browser:

    http://yourstreamingserver:8080/smithers2/domain/dans/user/getuigen

This is of course assuming that you have access to port 8080 of the server running Springfield. You may 
have to use an SSH-tunnel if this is not the case:

    ssh -L 8080:localhost:8080 yourstreamingserver
    
    # In a new terminal window:
    http://localhost:8080/smithers2/domain/dans/user/getuigen

The XML that is returned by Springfield will not be documented here, but for the most part is fairly 
easy to understand. Most of the subcommands supported by `easy-springfield` use this XML to implement
their functionality.

### Listing and creating containers
As discussed before, the videos are stored as leaves of a tree structure. The parent elements
of that structure can, in part, be examined and managed by `easy-springfield`. 

Currently it is only possible to list the users (in a given domain) and collections (of a given user).
To get a list of the items in a presentation, retrieve the raw Springfield metadata as explained in the previous
section.

Users and collections can be created with the subcommands `create-user` and `create-collection`. In
contrast to the command to add videos to Springfield - discussed below - these subcommands will
*not* create Springfield actions XML, but in fact create the specified items in Springfield directly.

### Adding videos 
Since Springfield can only add videos that are placed in its inbox, it is not possible to add them
directly via a subcommand. Instead, the subcommand `create-springfield-actions` generates the XML
that must be placed with the video files in the inbox. The input for this subcommand is a CSV file
containing the required metadata about the videos. The columns are defined by the following headers:

* `SRC-VIDEO` - the relative path to the video file in `springfield-inbox` 
* `DOMAIN` - (optional) the domain under which to add the video (must exist); if not specified: the default domain
* `USER` - the user under which to add the video (must exist)
* `COLLECTION` - the collection under which to add the video (must exist)
* `PRESENTATION` - the presentation under which to add the video (**must not** exist; will be created)
* `TARGET-VIDEO` - (optional) the name of the video in Springfield; if not specified: the base name of `SRC-VIDEO`
* `REQUIRE-TICKET` - whether an authorization ticket is required to play the video (`true` of `false`) 

The subcommand will print the XML directly to the standard output, so to use it you will have to redirect STDOUT
to a file. The OK/error messages will not interfere, as they will be printed on STDERR rather than STDOUT.

The subcommand can optionally check if the hierarchy in which to store the videos exists, as this is a
precondition for subsequent successful processing by Springfield. Likewise, it can check if the videos
actually exist in a specified folder. 

In summary, to add a folder with videos to Springfield you would perform the following steps:

1. Create a CSV-file (or you might start with a spreadsheet).
2. Add the column headers mentioned above (at least the mandatory ones).
3. Add to the `SRC-VIDEO` header: the paths of the videos relative to the *parent* of the folder that you plan to move 
  to the springfield-inbox.
4. Fill in the other columns to specify the desired situation in Springfield.
5. Save/export the CSV-file, say, to myvideos.csv
6. Run `easy-springfield -v /path/to/videos -p myvideos.csv > videos01.xml`. Now, if `/path/to/videos` contains one subdirectory
   called `videos01` and all the paths in `SRC-VIDEO` start with `videos01/...`  the tool will check if you are all
   set to move `videos01` to the springfield-inbox. (You may of course want to run this command first *without*
   redirecting the output to `videos01.xml`, to check if the generated XML looks OK.)
7. To be sure that there are no ownership issues `chmod 777` everything in `videos01` and also `videos01.xml`.
8. Move (or copy) the directory `videos01` to the springfield-inbox.
9. Move (or copy) the Springfield actions file `videos01.xml` to the springfield-inbox.

The Springfield service `uter` checks the inbox once a minute for new files. Processing the videos may take a while,
so at this point you might go off and take care of some other task. To check the status you can run the `status`
subcommand from time to time (see next section).

To debug any problems, it is best to examine `uter`'s  log file at `/var/log/tomcat/uter/uter.log`. 
 
### Status report
Processing the videos in the inbox takes a while. The `status` command generates a report that 
lists the A/V items and their current status (`DONE`, `WAITING` or `FAILED`). Note that items that have not (yet)
been successfully processed by Springfield's inbox service Uter will not appear in the status report at all.

### Changing require-ticket
You can change videos from public to private and vice versa with the `set-require-ticket` subcommand. This command
changes all the videos under a given parent, so you can change collections and users at a time. Before making the
actual changes you are requested to confirm.

### Creating and deleting tickets
For debugging purposes it is sometimes convenient to manually create authorization tickets for presentations. 
The `create-ticket` and `delete-ticket` enable you to do exactly that. You can optionally provided the ticket yourself and 
specify the number of seconds it must be valid. However, it is better to let easy-springfield generated the ticket for you,
as it will use a UUID, which tends to be more secure than any ticket number that you will think of yourself.

Viewing the list of current tickets is *not* supported. However, the Springfield Lenny service provides an HTML-based list
for this at `http://yourstreamingserver:8080/lenny/acl/ticket`.

### Deleting items
You can delete videos, presentations and collections with the `delete` subcommand. Users cannot currently be 
deleted, even if they contain no resources anymore. This seems a bug in Springfield, as&mdash;over time&mdash;it will inevitably
result in an increasing number of unused "user" resources. The `-r` option lets you include all the referenced resources
in a delete action. This way you can delete a whole collection without first having to manually delete all the 
resources referenced by it. Again, the user is asked to confirm such a delete action.

Please, note that the data on disk will *not* be cleaned up by Springfield. If you are using this command a lot you will be
faced with an increasing amount of wasted disk space.

### Fixing the Springfield hierarchy
To fix problems in the Springfield hierarchy the following subcommands can be used.

* `add-video-to-presentation` - this adds a video that is already in Springfield to a presentation.
* `add-presentation-to-collection` - this adds a presentation that is already in Springfield to a collection.

Together with the `delete` subcommand, used without the `-r` option, it is possible to repair the Springfield hierarchy, if it
becomes corrupted.

### Adding subtitles to existing videos
For videos that have been ingested without subtitles, you can still add those later. You will need to execute this command
as the `tomcat` user, so that it can write the subtitles files to the  `/data/dansstreaming` directory tree.

* `add-subtitles-to-video` - this adds a subtitles file to an existing video.
* `add-subtitles-to-presentation` - this adds subtitles files to the videos in an existing presentation.

[Springfield Web TV]: http://www.noterik.nl/products/webtv_framework/

ARGUMENTS
---------

        -h, --help      Show help message
          -v, --version   Show version of this program

        Subcommand: list-users - Lists users in a given domain
          -h, --help   Show help message

         trailing arguments:
          domain (not required)   the domain of which to list the users (default = dans)
        ---

        Subcommand: list-collections - Lists the collections of a user in a given domain
          -h, --help   Show help message

         trailing arguments:
          user (required)         the user whose collections to list
          domain (not required)   the domain containing the user (default = dans)
        ---

        Subcommand: create-user - Creates a new user in the Springfield database. This does NOT generate a springfield-actions XML but
        instead creates the user in Springfield right away.

          -h, --help   Show help message

         trailing arguments:
          user (required)         user name for the new user
          domain (not required)   the target domain in which to create the user
                                  (default = dans)
        ---

        Subcommand: create-collection - Creates a new collection in the Springfield database. This does NOT generate a springfield-actions XML but
        instead creates the collection in Springfield right away.

          -d, --description  <arg>   Description for the new collection (default = )
          -t, --title  <arg>         Title for the new collection (default = )
          -h, --help                 Show help message

         trailing arguments:
          collection (required)   name for the collection
          user (required)         existing user under which to store the collection
          domain (not required)   the target domain in which to create the collection
                                  (default = dans)
        ---

        Subcommand: create-presentation - Creates a new, empty presentation in the Springfield database, to be populated with the add-video-to-presentation command.

          -d, --description  <arg>   description for the new presentation (default = )
          -r, --require-ticket       whether to require a ticket before playing the
                                     presentation (private audio/video) or not (public
                                     audio/video)
          -t, --title  <arg>         title for the new presentation (default = )
          -h, --help                 Show help message

         trailing arguments:
          user (required)         existing user under which to store the collection
          domain (not required)   the target domain in which to create the presentation
                                  (default = dans)
        ---

        Subcommand: create-springfield-actions - Create Springfield Actions XML containing add-actions for A/V items specified in a CSV file
        with lines describing videos with the following columns: SRC, DOMAIN, USER, COLLECTION, PRESENTATION, FILE,
        REQUIRE-TICKET.

          -c, --check-parent-items     check that parent items (domain, user,
                                       collection) exist
          -v, --videos-folder  <arg>   folder relative to which to resolve the SRC
                                       column in the CSV
          -h, --help                   Show help message

         trailing arguments:
          video-csv (required)   CSV file describing the videos
        ---

        Subcommand: status - Retrieves the status of content offered for ingestion into Springfield.
          -d, --domain  <arg>   limit to videos within this domain (default = dans)
          -u, --user  <arg>     limit to videos owned by this user
          -h, --help            Show help message
        ---

        Subcommand: set-require-ticket - Sets or clears the 'require-ticket' flag for the specified presentation.
          -h, --help   Show help message

         trailing arguments:
          springfield-path (required)   the parent of items to change
          require-ticket (required)     true or false: whether to require a ticket
                                        before playing the presentation (private
                                        audio/video) or not (public audio/video)
        ---

        Subcommand: create-ticket - Creates and registers an authorization ticket for a specified presentation.
        If no ticket is specificied a random one is generated.
          -e, --expires-after-seconds  <arg>    (default = 300)
          -t, --ticket  <arg>                  the ticket to assign
          -h, --help                           Show help message

         trailing arguments:
          springfield-path (required)   the presentation to create the ticket for
        ---

        Subcommand: delete-ticket - Deletes a specified authorization ticket.
          -h, --help   Show help message

         trailing arguments:
          ticket (required)   the ticket to delete
        ---

        Subcommand: delete - Deletes the item at the specified Springfield path.
          -r, --with-referenced-items   also remove items reference from <path>,
                                        recursively
          -h, --help                    Show help message

         trailing arguments:
          path (required)   the path pointing item to remove
        ---

        Subcommand: add-videoref-to-presentation - Adds a videoref to a presentation under a specified name. The video must already exist in Springfield.
          -h, --help   Show help message

         trailing arguments:
          video (required)          referid of the video
          name (required)           name to assign to the video in the presentation
          presentation (required)   the presentation, either a Springfield path or a
                                    referid
        ---

        Subcommand: add-presentationref-to-collection - Adds a presentation to a collection under a specified name. The presentation must already exist in Springfield
          -h, --help   Show help message

         trailing arguments:
          presentation (required)   referid of the presentation
          name (required)           name to assign to the presentation in the collection
          collection (required)     the Springfield path of the collection
        ---

        Subcommand: add-subtitles-to-video - Adds a subtitles file to an existing video.
          -l, --language  <arg>   the ISO 639-1 (two letter) language code
          -h, --help              Show help message

         trailing arguments:
          video (required)         the referid of the video
          webvtt-file (required)   path to the WebVTT subtitles file to add
        ---

        Subcommand: add-subtitles-to-presentation -
         Adds one or more subtitles file(s) to an existing presentation. If the presentation contains multiple videos
         the same number of WebVTT files must be specified; they will be added in the specified order to the respective videos.

          -l, --language  <arg>   the ISO 639-1 (two letter) language code
          -h, --help              Show help message

         trailing arguments:
          presentation (required)     referid of the presentation
          webvtt-file(s) (required)   path to the WebVTT subtitles file(s) to add
        ---

INSTALLATION AND CONFIGURATION
------------------------------
The preferred way of install this module is using the RPM package. This will install the binaries to
`/opt/dans.knaw.nl/easy-springfield`, the configuration files to `/etc/opt/dans.knaw.nl/easy-springfield`,
and will install the service script for `initd` or `systemd`. It will also set up a default bag store
at `/srv/dans.kanw.nl/bag-store`.

If you are on a system that does not support RPM, you can use the tarball. You will need to copy the
service scripts to the appropiate locations yourself.

BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher
* RPM (if you want to build the RPM package).

Steps:

    git clone https://github.com/DANS-KNAW/easy-springfield.git
    cd easy-springfield
    mvn install

If the `rpm` executable is found at `/usr/local/bin/rpm`, the build profile that includes the RPM
packaging will be activated. If `rpm` is available, but at a different path, then activate it by using
Maven's `-P` switch: `mvn -Pprm install`.

