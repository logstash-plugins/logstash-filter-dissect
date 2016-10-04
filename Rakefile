require "logstash/devutils/rake"

require 'rubygems'
require 'rubygems/package_task'
require 'fileutils'
require 'rspec/core/rake_task'

# Please read BUILD_INSTRUCTIONS.md

desc "Compile and vendor java into ruby"
task :vendor => [:bundle_install] do
  `./gradlew vendor`
end

desc "Do bundle install and write gradle.properties"
task :bundle_install do
  `bundle install`
  lsc_path = `bundle show logstash-core`
  lsce_path = `bundle show logstash-core-event`
  rm_f("./gradle.properties")
  open("./gradle.properties", "w") do |f|
    f.puts "logstashCoreGemPath=#{lsc_path}"
    f.puts "logstashCoreEventGemPath=#{lsce_path}"
  end
end

desc "Write gradle.properties" # used by travis
task :write_gradle_properties do
  lsc_path = `bundle show logstash-core`
  lsce_path = `bundle show logstash-core-event`
  rm_f("./gradle.properties")
  open("./gradle.properties", "w") do |f|
    f.puts "logstashCoreGemPath=#{lsc_path}"
    f.puts "logstashCoreEventGemPath=#{lsce_path}"
  end
end

spec = Gem::Specification.load('logstash-filter-dissect.gemspec')
Gem::PackageTask.new(spec) do
  desc 'Package gem'
  task :package => [:vendor]
end

RSpec::Core::RakeTask.new(:spec)
task :check => [:vendor, :spec]
