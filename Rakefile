require "logstash/devutils/rake"

require 'rubygems'
require 'rubygems/package_task'
require 'fileutils'
require 'rspec/core/rake_task'

# Please read BUILD_INSTRUCTIONS.md

desc "Compile and vendor java into ruby"
task :vendor => [:bundle_install] do
  sh("./gradlew check vendor")
  puts "-------------------> vendored dissect jar via rake"
end

desc "Compile and vendor java into ruby for travis, its done bundle install already"
task :travis_vendor => [:write_gradle_properties] do
  sh("./gradlew check vendor")
  puts "-------------------> vendored dissect jar via rake"
end

desc "Do bundle install and write gradle.properties"
task :bundle_install do
  `bundle install`
  delete_create_gradle_properties
end

desc "Write gradle.properties" # used by travis
task :write_gradle_properties do
  delete_create_gradle_properties
end

spec = Gem::Specification.load('logstash-filter-dissect.gemspec')
Gem::PackageTask.new(spec) do
  desc 'Package gem'
  task :package => [:vendor]
end

RSpec::Core::RakeTask.new(:spec)
task :check => [:vendor, :spec]

task :travis_test => [:travis_vendor, :spec]

def delete_create_gradle_properties
  root_dir = File.dirname(__FILE__)
  gradle_properties_file = "#{root_dir}/gradle.properties"
  lsc_path = `bundle show logstash-core`
  FileUtils.rm_f(gradle_properties_file)
  File.open(gradle_properties_file, "w") do |f|
    f.puts "logstashCoreGemPath=#{lsc_path}"
  end
  puts "-------------------> Wrote #{gradle_properties_file}"
  puts `cat #{gradle_properties_file}`
end


