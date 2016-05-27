# encoding: utf-8
require "logstash/filters/base"
require "logstash/namespace"

require "java"
require "jars/jruby-dissect-library.jar"
require "jruby_dissect"

# This example filter will replace the contents of the default
# message field with whatever you specify in the configuration.
#
# It is only intended to be used as an example.
class LogStash::Filters::Dissect < LogStash::Filters::Base

  # Setting the config_name here is required. This is how you
  # configure this filter from your Logstash config.
  #
  # filter {
  #   example {
  #     message => "My message..."
  #   }
  # }
  #
  config_name "dissect"

  # Replace the message with this value.
  config :mapping, :validate => :hash, :default => {}


  public

  def register
    # check for empty mapping hash
  end

  def filter(event)
    @logger.debug? && @logger.debug("Event before dissection", "event" => event)
    @mapping.each do |src_field, mapped|
      dissector = LogStash::Dissect.new(event, event.get(src_field), mapped)
      dissector.dissect
    end
    @logger.debug? && @logger.debug("Event after dissection", "event" => event)
    # filter_matched should go in the last line of our successful code
    filter_matched(event)
  end
end
