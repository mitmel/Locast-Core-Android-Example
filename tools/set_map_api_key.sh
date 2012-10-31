#!/bin/sh

key="$1"

sed -i -e "/edu.mit.mobile.android.maps.GOOGLE_STATIC_MAPS_API_KEY/ { n; c \
            android:value=\"${key}\" />
 }" `dirname $0`/../AndroidManifest.xml

