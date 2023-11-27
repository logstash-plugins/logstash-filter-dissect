require "logstash/devutils/rake"

require 'rubygems'
require 'rubygems/package_task'
require 'fileutils'
require 'rspec/core/rake_task'

# Please read BUILD_INSTRUCTIONS.md

def check_gemspec_version
  root_dir = File.dirname(__FILE__)
  dissect_version = File.read(File.expand_path(File.join(root_dir, "VERSION"))).strip
  gemspec = File.read(File.expand_path(File.join(root_dir, "logstash-filter-dissect.gemspec")))
  spec_version = eval(gemspec, TOPLEVEL_BINDING).version.to_s
  dissect_version == spec_version
end

desc "Compile and vendor java into ruby"
task :vendor => [:bundle_install] do
  puts "-------------------> checking gemspec version matches VERSION file"
  exit(1) unless check_gemspec_version
  exit(1) unless system './gradlew check vendor'
  puts "-------------------> vendored dissect jar via rake"
end

desc "Compile and vendor java into ruby for travis, its done bundle install already"
task :travis_vendor => [:write_gradle_properties] do
  puts "-------------------> checking gemspec version matches VERSION file"
  exit(1) unless check_gemspec_version
  exit(1) unless system './gradlew check vendor'
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

desc "Run full check with custom Logstash path"
task :custom_ls_check, :ls_dir do |task, args|
  ls_path = args[:ls_dir]
  system(custom_ls_path_shell_script(ls_path))
end

def custom_ls_path_shell_script(path)
  # Use same JRuby that launched this Rake
  current_ruby_path = RbConfig::CONFIG['prefix']
  <<TXT
export LOGSTASH_PATH='#{path}'
export LOGSTASH_SOURCE=1
#{current_ruby_path}/bin/jruby -S bundle install
#{current_ruby_path}/bin/jruby -S bundle exec rake travis_vendor
#{current_ruby_path}/bin/jruby -S bundle exec rspec spec
TXT
end

def delete_create_gradle_properties
  root_dir = File.dirname(__FILE__)
  gradle_properties_file = "#{root_dir}/gradle.properties"
  # find the path to the logstash-core gem
  lsc_path = Bundler.rubygems.find_name("logstash-core").first.full_gem_path

  FileUtils.rm_f(gradle_properties_file)
  File.open(gradle_properties_file, "w") do |f|
    f.puts "logstashCoreGemPath=#{lsc_path}"
  end
  puts "-------------------> Wrote #{gradle_properties_file}"
  puts `cat #{gradle_properties_file}`
end
