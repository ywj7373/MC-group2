# Interactive Productivity Manager App

Jinyeong Kim : jinyeongkim37@gmail.com / sonia37@snu.ac.kr<br />
YoungWoong Jun: herojun9696@gmail.com / ywj7373@snu.ac.kr<br />
Sigrid Marita Kvamme: sigridkvamme@gmail.com

## UI

- Learn about how to make user interfaces in Android through tutorials
- UI mockup sketch

## Usage Tracking + App blocking

- Reading usage data for app
- Analysing usage: Trigger app blocking based on app usage pattern (figure out smart sensing of when user might be procrastinating) (differentiating feature from competition)
- Baseline: Block app based on users set restriction (default 40 min)
- Send notification 5 min before block warning that if you use if for longer it will get blocked
- Investigate what data we can get from the phone through ActivityManager
- Develop a smarter algorithm for detecting procrastinating (next step)
- Send warning notification in advance of blocking app (5 min before) saying that if you use it for longer, it will get blocked
- Reset block timer when user has stopped using app for duration app would be blocked in the setting (default 5 min)
- How to block an app, how to unblock app
- Create screen for blocked app
- Investigate difficulty of creating pop-up modal to show requirements to unlock app (show timer, pedometer requirement progress).
- Fallback if too difficult is to open in app and show it there.
- Permission to use pedometer
- Reading step count
- Set criteria for unblocking app
- Set time duration of block, default 5 minutes (make a setting later to let user change)
- When to block, default for all (can be expanded later to allow different apps to have different restrictions)
- Set walking requirement
- Create to-do list to enable activation of this mode only on specific times (so it does not turn on when you actually have nothing you need to do and can spend as much time on your phone as you want).

## Location alert

- Reading GPS data
- Getting permission to use GPS
- Investigate battery saving policies
- Implement some form of power conservation for GPS
- Estimating travel time
- Look for Google Maps Directions API or Kakao Map API or Naver API
- Use API to get estimated travel time
- Compare current device location with planned location (Are you at the right place?) (Is current coordinates within the accepted coordinate range? (broadened acceptance area)
- Send notification in advance of time you are supposed to be at location
- Estimated travel time (commuting)
- Activate GPS 1 hour before scheduled location reminder
- Check every 10 minutes for location and estimate travel time
- Use that estimate to decide when to send out reminder
- Check where you are 40 min before class and see if travel time is long
- Send notification at set time you should be at location (“You made it! [score +1]” or “You missed ~~~,”
  (Note: No reason to go if you already missed it? Could feel meaningless to some. Maybe something ala eAttendance, Present (interval of time accepted), Late-in (can still get reward for still getting there), Absent)
  If missed time exactly, notification says “Hurry up! You can still make it! 10 min left to get full score (1p)”
  If you make it in time then, new notification “You made it! Score +1. Estimated grade A”  
  Keep score count (Decide scoring system/gamification)
- Cumulative kind of grading estimating
- Display current score (grade)
- Could add different statistics (nice to have, not prioritised if time consuming)
- List of location reminders set up
- Make a timetable(with location) functionality (advanced level, nice to have but high complexity)

## Homework mode

- Block all apps selected in settings
- Send notification with check-in prompt
- Check in through app or notification
- Permission to use motion data?
- Check in through movement (e.g. shaking, flipping phone)
- Investigate how to use gestures through movement
- Permission to use alarm?
- Activate alarm on no check in
- Turn on homework mode
- Turn off homework mode
- Block turn off if tasks due soon that have not been completed? (could be very annoying if you actually cannot do more homework at that time, but will do it later)
- Input from user “I have done my homework”

### Configure homework mode

- Select apps to be blocked on activation of HW mode
- Select interval of check-in time
- Button to turn on/turn off
