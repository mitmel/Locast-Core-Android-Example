Open Locast Core Example
========================

[Open Locast Core][locastcore] is an Android library that works in conjunction
with the [Open Locast web platform][locastwebcore]. It is a framework for
building location-based media apps, with the ability to synchronize data
to/from the web platform.

This application is a fully-working example of using the Open Locast Core
and is intended to be used alongside [Open Locast Web][locastweb].

Out of the box, Open Locast Core Example provides the ability to login to an
Open Locast Web site.

Once logged in, the application can be used both online and offline, allowing
content to be created, deleted, etc. on the device without a network
connection. Changes will be synchronized with the server once a network
connection has been restored.

Content in Open Locast Web / this application is broken up into Casts and
Collections.

### Casts

A cast is a geo-located collection of media which can contain one or more
pictures/videos. You can imagine a cast as a single location, object, or event,
perhaps with different viewpoints or angles.

Casts have a privacy restriction. They can be marked public, protected or
private. Public casts can be edited by any user. Protected casts can only be
edited by the original author, but seen by anyone. Private casts are only
visible and editable by the original author.

Casts have titles, descriptions, and allow for free tagging.

### Collections

A collection is a set of casts. They are intended to be highly generic, with a
use defined by the user. They can have titles and descriptions and feature the
same privacy restrictions as casts.

Building
--------

This application depends on a number of libraries to function. Some are
provided in the `libs/` directory, but a number are provided as git submodules.
You can check out the complete project using the following command:

    git clone --recurse-submodules https://github.com/mitmel/Locast-Core-Android-Example.git

Please see the README of [Open Locast Core][locastcore] for more details on how
to build the library.

Server Communication
--------------------

The application communicates with the server using a public RESTful API,
allowing both authenticated and unauthenticated API requests.

The API's base URL is stored in your application's manifest. You can add it in
by adding a block similar to this to your `<application />` section of your
`AndroidManifest.xml`:

        <meta-data
            android:name="edu.mit.mobile.android.locast.base_url"
            android:value="http://example.com/openlocast/api/" />

Please see [Locast Web][locastweb] for more details on how to set up the server.

License
-------
Locast Android Core Example  
Copyright 2010-2013 [MIT Mobile Experience Lab][mel]

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

[mel]: http://mobile.mit.edu/
[locastweb]: https://github.com/mitmel/OpenLocast-Web
[locastwebcore]: https://github.com/mitmel/Locast-Web-Core
[locastcore]: https://github.com/mitmel/Locast-Core-Android
