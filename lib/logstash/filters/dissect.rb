# encoding: utf-8
require "logstash/filters/base"
require "logstash/namespace"

require "java"
require "jars/jruby-dissect-library-all.jar"
require "jruby_dissect"

# De-structures text
#
# The dissect filter is a kind of split operation.
# Unlike a regular split operation where a single delimiter is applied to the
# whole string, this operation applies a sequence of delimiters to an Event field's
# string value. This sequence is called a dissection.
# The dissection is created as a string using a %{} notation:
# ........
#        delimiter   suffix
#        +---+       ++
# %{key1}/ -- %{+key2/1}: %{&key1}
# +-----+       |            +--+
# field         prefix       key
# ........

# Note: delimiters can't contain the `%{` `}` characters.

# The config should look like this:
# [source, ruby]
#   filter {
#     dissect {
#       mapping => {
#         "message" => "%{timestamp} %{+timestamp} %{+timestamp} %{logsource} %{} %{program}[%{pid}]: %{msg}"
#       }
#     }
#   }

# When dissecting a string any text between the delimiters, a found value, will be stored
# in the Event using that field name.

# The Key:
# The key is the text between the `%{` and `}`, exclusive of the ?, +, & prefixes and the ordinal suffix.
# `%{?aaa}` - key is `aaa`
# `%{+bbb/3}` - key is `bbb`
# `%{&ccc}` - key is `ccc`

# Normal field notation
# The found value is added to the Event using the key.
# `%{some_field}` - a normal field

# Skip field notation
# The found value is recorded internally but not added to the Event.
# The key, if supplied, is prefixed with a `?`.
# `%{}` - an empty skip field
# `%{?some_field} - named skip field

# Append field notation
# The value is appended to another value or stored if its the first field seen.
# The key is prefixed with a `+`.
# The final value is stored in the Event using the key.
# The delimiter found before the field or a space is appended before the found value.
# `%{+some_field}` - an append field
# `%{+some_field/2}` - and append field with an order modifier.
# An order modifier, `/number`, allows one to reorder the append sequence.
# e.g. for a text of `1 2 3 go`, this `%{+a/2} %{+a/1} %{+a/4} %{+a/3}` will build a key/value of `a => 2 1 go 3`
# Append fields without an order modifier will append in declared order.
# e.g. for a text of `1 2 3 go`, this `%{a} %{b} %{+a}` will build two key/values of `a => 1 3 go, b => 2`

# Indirect field notation
# The found value is added to the Event using the found value of another field as the key.
# The key is prefixed with a `&`.
# `%{&some_field}` - an indirect field where the key is indirectly sourced from the value of `some_field`.
# e.g. for a text of `error: some_error, description`, this `error: %{?err}, %{&desc}`will build a key/value of `'an error' => description`
# Hint: use a Skip field if you do not want the indirection key/value stored.
# e.g. for a text of `google: 77.98`, this `%{?a}: %{&a}` will build a key/value of `google => 77.98`.

# Note: for append and indirect field the key can refer to a field that already exists in the event before dissection.
# Note: append and indirect cannot be combined...
# `%{+&something}` - will add a value to the `&something` key, probably not the intended outcome.
# `%{&+something}` will add a value to the `+something` key, again unintended.

# Delimiter repetition
# In the source text if a field has variable width padded with delimiters, the padding will be ignored.
# e.g. for texts of:
# ........
# 00000043 ViewReceiver  I
# 000000b3 Peer          I
# ........
# and a dissection of `%{a} %{b} %{c}`; the padding is ignored.
#
# You probably want to put this filter in an if block to ensure that the event
# contains text with a suitable layout.
# [source, ruby]
# filter {
#   if [type] == "syslog" or "syslog" in [tags] {
#     dissect {
#       mapping => {
#         "message" => "%{timestamp} %{+timestamp} %{+timestamp} %{logsource} %{} %{program}[%{pid}]: %{msg}"
#       }
#     }
#   }
# }

class LogStash::Filters::Dissect < LogStash::Filters::Base

  config_name "dissect"

  # A hash of dissections of field => value
  # A later dissection can be done on an earlier one
  # or they can be independent.
  #
  # For example
  # [source, ruby]
  # filter {
  #   dissect {
  #     mapping => {
  #       "message" => "%{field1} %{field2} %{description}"
  #       "description" => "%{field3} %{field4} %{field5}"
  #     }
  #   }
  # }
  #
  # This is useful if you want to keep the field `description` also
  # dissect it some more.
  config :mapping, :validate => :hash, :default => {}

  # Append values to the `tags` field when dissection fails
  config :tag_on_failure, :validate => :array, :default => ["_dissectfailure"]

  public

  def register
    @dissector = LogStash::Dissector.new(@mapping)
  end

  def filter(event)
    # all plugin functions happen in the JRuby extension:
    # debug, warn and error logging, filter_matched, tagging etc.
    @dissector.dissect(event, self)
  end

  def multi_filter(events)
    LogStash::Util.set_thread_plugin(self)
    @dissector.dissect_multi(events, self)
    events
  end
end
