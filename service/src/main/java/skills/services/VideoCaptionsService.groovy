/**
 * Copyright 2020 SkillTree
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
package skills.services

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

@Service
@Slf4j
class VideoCaptionsService {

    String getVideoCaptions(String videoId) {
        String res = '''WEBVTT

1
00:00:00.500 --> 00:00:04.000
Hello, Welcome to Tutorialspoint!

2
00:00:04.100 --> 00:00:06.000
This is a demo video.

3
00:00:06.100 --> 00:00:10.500
We have used a WEBVTT file to show the subtitles.

4
00:00:10.600 --> 00:00:14.000
Audio won't match the subtitle.

5
00:00:14.100 --> 00:00:17.500
It's just a demo to check if we

6
00:00:17.600 --> 00:00:19.500
show subtitles in English in Video.js

7
00:00:19.600 --> 00:00:21.500
Not just English,

8
00:00:21.600 --> 00:00:24.100
you can show subtitles

9
00:00:24.200 --> 00:00:26.500
in any language of your preference.

10
00:00:26.600 --> 00:00:29.500


11
00:00:29.600 --> 00:00:33.500
hhmm... hhmmm... hmmm...

12
00:00:33.600 --> 00:00:34.000
FRANK!!!'''
       return res
    }
}
